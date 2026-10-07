package com.emmagax.eden.media;

import com.emmagax.eden.model.User;
import com.emmagax.eden.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import java.util.UUID;
import java.util.Map;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"eden.media.enabled=true", "eden.media.bucket=test"})
@AutoConfigureMockMvc
@Testcontainers
class MediaIntegrationTest {
  @Container @ServiceConnection static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");
  @MockitoBean ObjectStorage storage;
  @Autowired MediaRepository repository;
  @Autowired JdbcTemplate jdbc;
  @Autowired MockMvc mvc;
  @Autowired UserRepository users;
  User account() {
    User user = new User(); String name = UUID.randomUUID().toString();
    user.setUsername(name); user.setEmail(name + "@example.com"); return users.save(user);
  }
  MediaAsset asset(User owner) {
    UUID id = UUID.randomUUID();
    var asset = new MediaAsset(id, owner.getId(), "uploads/" + id, "audio/wav", 16,
        "0".repeat(64), 30, "UPLOADING", 0, null, null, null, null, null);
    repository.insert(asset); return asset;
  }
  @Test void uploadIsAuthenticatedValidatedAndOwned() throws Exception {
    User owner = account();
    String body = "{\"contentType\":\"audio/wav\",\"sizeBytes\":16,\"sha256\":\"" + "0".repeat(64) + "\"}";
    mvc.perform(post("/media/uploads").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnauthorized());
    mvc.perform(post("/media/uploads").with(user(owner.getUsername())).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
    mvc.perform(post("/media/uploads").with(user(owner.getUsername())).with(csrf()).contentType(MediaType.APPLICATION_JSON)
        .content(body.replace("audio/wav", "text/html"))).andExpect(status().isBadRequest());
    mvc.perform(post("/media/uploads").with(user(owner.getUsername())).with(csrf()).contentType(MediaType.APPLICATION_JSON)
        .content(body.replace("16", "104857601"))).andExpect(status().isBadRequest());
    when(storage.signUpload(any())).thenReturn(new ObjectStorage.SignedUpload("https://storage.test/signed", Map.of()));
    mvc.perform(post("/media/uploads").with(user(owner.getUsername())).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.uploadUrl").value("https://storage.test/signed"));
    verify(storage).signUpload(argThat(media -> media.ownerId() == owner.getId()));
  }
  @Test void schemaEnforcesMediaAndCatalogConstraints() {
    var asset = asset(account());
    assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
        () -> jdbc.update("UPDATE media_assets SET state='READY' WHERE id=?", asset.id()));
    assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
        () -> jdbc.update("UPDATE media_assets SET size_bytes=0 WHERE id=?", asset.id()));
  }
}
