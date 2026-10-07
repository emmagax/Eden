package com.emmagax.eden.media;

import com.emmagax.eden.model.User;
import com.emmagax.eden.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import java.nio.file.Path;
import java.nio.file.Files;
import java.net.URI;
import java.net.http.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import java.util.Map;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.jayway.jsonpath.JsonPath;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@EnabledIfEnvironmentVariable(named="EDEN_TEST_STORAGE", matches="true")
@SpringBootTest(properties={"eden.media.enabled=true", "eden.media.bucket=eden-media",
    "eden.media.endpoint=http://localhost:9000", "eden.media.poll-ms=100",
    "eden.media.ffmpeg=${EDEN_TEST_FFMPEG}", "eden.media.ffprobe=${EDEN_TEST_FFPROBE}"})
@AutoConfigureMockMvc
@org.springframework.test.annotation.DirtiesContext(classMode=org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Testcontainers
class MediaStorageSmokeTest {
  @Container @ServiceConnection static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");
  @TempDir Path directory;
  @Autowired MockMvc mvc;
  @Autowired UserRepository users;
  @Autowired MediaRepository repository;
  @Test void directUploadProcessesRealAudioAndServesPrivateRangePlayback() throws Exception {
    User owner = new User(); String name = "smoke" + UUID.randomUUID().toString().substring(0, 8);
    owner.setUsername(name); owner.setEmail(name + "@example.com"); users.save(owner);
    Path input = directory.resolve("test.wav");
    var generate = new ProcessBuilder(System.getenv("EDEN_TEST_FFMPEG"), "-v", "error", "-f", "lavfi", "-i",
        "sine=frequency=440:duration=8", input.toString()).redirectErrorStream(true)
        .redirectOutput(directory.resolve("generate.log").toFile()).start();
    assertTrue(generate.waitFor(20, TimeUnit.SECONDS)); assertEquals(0, generate.exitValue());
    byte[] bytes = Files.readAllBytes(input);
    String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    var created = mvc.perform(post("/media/uploads").with(user(name)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
        .content("{\"contentType\":\"audio/wav\",\"sizeBytes\":" + bytes.length + ",\"sha256\":\"" + hash + "\",\"previewSeconds\":5}"))
        .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    UUID id = UUID.fromString(JsonPath.read(created, "$.id"));
    String uploadUrl = JsonPath.read(created, "$.uploadUrl");
    Map<String, List<String>> headers = JsonPath.read(created, "$.headers");
    var client = HttpClient.newHttpClient();
    var preflight = client.send(HttpRequest.newBuilder(URI.create(uploadUrl))
        .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).header("Origin", "http://localhost:5173")
        .header("Access-Control-Request-Method", "PUT").header("Access-Control-Request-Headers", "content-type")
        .build(), HttpResponse.BodyHandlers.discarding());
    assertTrue(preflight.statusCode() >= 200 && preflight.statusCode() < 300);
    assertEquals("http://localhost:5173", preflight.headers().firstValue("Access-Control-Allow-Origin").orElseThrow());
    var put = HttpRequest.newBuilder(URI.create(uploadUrl)).PUT(HttpRequest.BodyPublishers.ofByteArray(bytes));
    headers.forEach((key, values) -> {
      if (!key.equalsIgnoreCase("host") && !key.equalsIgnoreCase("content-length")) values.forEach(value -> put.header(key, value));
    });
    assertEquals(200, client.send(put.build(), HttpResponse.BodyHandlers.discarding()).statusCode());
    var asset = repository.find(id).orElseThrow();
    assertEquals(403, client.send(HttpRequest.newBuilder(URI.create("http://localhost:9000/eden-media/" + asset.uploadKey()))
        .GET().build(), HttpResponse.BodyHandlers.discarding()).statusCode());
    mvc.perform(post("/media/" + id + "/complete").with(user(name)).with(csrf())).andExpect(status().isOk());
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
    do {
      asset = repository.find(id).orElseThrow();
      if (asset.state().equals("READY") || asset.state().equals("FAILED")) break;
      Thread.sleep(100);
    } while (System.nanoTime() < deadline);
    assertEquals("READY", asset.state(), asset.failureCode());
    var playback = mvc.perform(get("/media/" + id + "/playback").with(user(name))).andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    String stream = JsonPath.read(playback, "$.streamUrl"), preview = JsonPath.read(playback, "$.previewUrl");
    var ranged = client.send(HttpRequest.newBuilder(URI.create(stream)).header("Range", "bytes=0-127").GET().build(), HttpResponse.BodyHandlers.ofByteArray());
    assertEquals(206, ranged.statusCode()); assertEquals(128, ranged.body().length);
    byte[] previewBytes = client.send(HttpRequest.newBuilder(URI.create(preview)).GET().build(), HttpResponse.BodyHandlers.ofByteArray()).body();
    assertTrue(previewBytes.length > 1000);
  }
}
