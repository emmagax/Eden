package com.emmagax.eden;

import com.emmagax.eden.model.User;
import com.emmagax.eden.model.Profile;
import com.emmagax.eden.repository.UserRepository;
import com.emmagax.eden.repository.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthorizationIntegrationTest {
  @Container @ServiceConnection static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");
  @Autowired MockMvc mvc;
  @Autowired UserRepository users;
  @Autowired ProfileRepository profiles;
  @Autowired PasswordEncoder encoder;
  User owner; User other; Profile profile; Profile otherProfile;
  @BeforeEach void setup() {
    String suffix = java.util.UUID.randomUUID().toString().substring(0, 8);
    owner = account("owner" + suffix); other = account("other" + suffix);
    profile = profile(owner); otherProfile = profile(other);
  }
  User account(String name) {
    User user = new User(); user.setUsername(name); user.setEmail(name + "@example.com");
    user.setPassword(encoder.encode("Password123!")); return users.save(user);
  }
  Profile profile(User user) {
    Profile profile = new Profile(); profile.setUser(user); profile.setArtistName(user.getUsername());
    profile.setHandle(user.getUsername()); return profiles.save(profile);
  }
  String profileBody() { return "{\"artistName\":\"Artist\",\"handle\":\"" + owner.getUsername() + "\",\"bio\":\"Updated\",\"onboardingComplete\":false}"; }
  @Test void profileUpdateAllowsOwnerAndDeniesOthersWithoutChangingData() throws Exception {
    mvc.perform(put("/profiles/" + profile.getId()).with(user(other.getUsername())).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content(profileBody())).andExpect(status().isForbidden());
    assertNull(profiles.findById(profile.getId()).orElseThrow().getBio());
    mvc.perform(put("/profiles/" + profile.getId()).with(user(owner.getUsername())).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content(profileBody())).andExpect(status().isOk());
    assertEquals("Updated", profiles.findById(profile.getId()).orElseThrow().getBio());
  }
  @Test void profileCreationUsesAuthenticatedAccount() throws Exception {
    User third = account("third" + java.util.UUID.randomUUID().toString().substring(0,8));
    String body = "{\"artistName\":\"Third\",\"handle\":\"" + third.getUsername() + "\"}";
    mvc.perform(post("/profiles/users/" + third.getId() + "/profile").with(user(owner.getUsername())).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
    mvc.perform(post("/profiles/users/" + third.getId() + "/profile").with(user(third.getUsername())).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.user.id").value(third.getId()));
  }
  @Test void userUpdateCannotModifyAnotherAccount() throws Exception {
    String body = "{\"email\":\"changed@example.com\",\"username\":\"" + owner.getUsername() + "\"}";
    mvc.perform(put("/users/" + owner.getId()).with(user(other.getUsername())).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
    assertNotEquals("changed@example.com", users.findById(owner.getId()).orElseThrow().getEmail());
    mvc.perform(put("/users/" + owner.getId()).with(user(owner.getUsername())).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.password").doesNotExist());
  }
  @Test void connectionSenderMustBelongToAuthenticatedUser() throws Exception {
    String body = "{\"fromProfileId\":" + profile.getId() + ",\"toProfileId\":" + otherProfile.getId() + "}";
    mvc.perform(post("/connection-requests").with(user(other.getUsername())).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
    mvc.perform(post("/connection-requests").with(user(owner.getUsername())).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
  }
  @Test void anonymousMutationsAreDeniedEvenWithCsrf() throws Exception {
    for (String path : new String[]{"/profiles/users/1/profile", "/connection-requests", "/auth/logout", "/auth/email-verification/request"})
      mvc.perform(post(path).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
    for (String path : new String[]{"/users/1", "/profiles/1"})
      mvc.perform(put(path).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
  }
  @Test void csrfIsRequiredForEveryPostAndPut() throws Exception {
    for (String path : new String[]{"/auth/register", "/auth/login", "/auth/logout", "/auth/email-verification/request",
        "/auth/email-verification/confirm", "/auth/password-reset/request", "/auth/password-reset/confirm", "/connection-requests", "/profiles/users/1/profile"})
      mvc.perform(post(path).with(user(owner.getUsername())).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
    for (String path : new String[]{"/users/1", "/profiles/1"})
      mvc.perform(put(path).with(user(owner.getUsername())).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
  }
  @Test void loginRestoresSessionRotatesIdAndLogoutInvalidatesIt() throws Exception {
    MockHttpSession session = new MockHttpSession(); String oldId = session.getId();
    var result = mvc.perform(post("/auth/login").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
        .content("{\"identifier\":\"" + owner.getUsername() + "\",\"password\":\"Password123!\"}"))
        .andExpect(status().isOk()).andReturn();
    MockHttpSession signedIn = (MockHttpSession) result.getRequest().getSession(false);
    assertNotEquals(oldId, signedIn.getId());
    mvc.perform(get("/auth/me").session(signedIn)).andExpect(status().isOk()).andExpect(jsonPath("$.username").value(owner.getUsername()));
    mvc.perform(post("/auth/logout").session(signedIn).with(csrf())).andExpect(status().isNoContent());
    assertTrue(signedIn.isInvalid());
    mvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
  }
  @Test void resetTokenIsNeverReturnedWithProductionDefaults() throws Exception {
    mvc.perform(post("/auth/password-reset/request").with(csrf()).contentType(MediaType.APPLICATION_JSON)
        .content("{\"identifier\":\"" + owner.getUsername() + "\"}"))
        .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.token").doesNotExist());
  }

  String tokenHash(String token) throws Exception {
    return java.util.Base64.getEncoder().encodeToString(java.security.MessageDigest.getInstance("SHA-256")
        .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
  }
  @Test void passwordResetRequiresUnexpiredSingleUseToken() throws Exception {
    String token = java.util.UUID.randomUUID().toString();
    owner.setPasswordResetTokenHash(tokenHash(token));
    owner.setPasswordResetTokenExpiresAt(java.time.LocalDateTime.now().minusMinutes(1)); users.save(owner);
    String body = "{\"token\":\"" + token + "\",\"newPassword\":\"NewPassword123!\"}";
    mvc.perform(post("/auth/password-reset/confirm").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest());
    assertTrue(encoder.matches("Password123!", users.findById(owner.getId()).orElseThrow().getPassword()));
    owner.setPasswordResetTokenExpiresAt(java.time.LocalDateTime.now().plusMinutes(1)); users.save(owner);
    mvc.perform(post("/auth/password-reset/confirm").with(csrf()).contentType(MediaType.APPLICATION_JSON)
        .content(body.replace(token, "wrong-token"))).andExpect(status().isBadRequest());
    mvc.perform(post("/auth/password-reset/confirm").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isNoContent());
    var saved = users.findById(owner.getId()).orElseThrow();
    assertTrue(encoder.matches("NewPassword123!", saved.getPassword())); assertNull(saved.getPasswordResetTokenHash());
    mvc.perform(post("/auth/password-reset/confirm").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest());
  }
  @Test void emailVerificationRequiresUnexpiredSingleUseToken() throws Exception {
    String token = java.util.UUID.randomUUID().toString();
    owner.setEmailVerificationTokenHash(tokenHash(token));
    owner.setEmailVerificationTokenExpiresAt(java.time.LocalDateTime.now().minusMinutes(1)); users.save(owner);
    String body = "{\"token\":\"" + token + "\"}";
    mvc.perform(post("/auth/email-verification/confirm").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest());
    assertFalse(users.findById(owner.getId()).orElseThrow().isEmailVerified());
    owner.setEmailVerificationTokenExpiresAt(java.time.LocalDateTime.now().plusMinutes(1)); users.save(owner);
    mvc.perform(post("/auth/email-verification/confirm").with(csrf()).contentType(MediaType.APPLICATION_JSON)
        .content(body.replace(token, "wrong-token"))).andExpect(status().isBadRequest());
    mvc.perform(post("/auth/email-verification/confirm").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isNoContent());
    assertTrue(users.findById(owner.getId()).orElseThrow().isEmailVerified());
    mvc.perform(post("/auth/email-verification/confirm").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest());
  }
  @Test void loginRejectsInvalidCredentialsWithoutCreatingAuthenticatedSession() throws Exception {
    mvc.perform(post("/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
        .content("{\"identifier\":\"" + owner.getUsername() + "\",\"password\":\"WrongPassword\"}"))
        .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    mvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
  }
}
