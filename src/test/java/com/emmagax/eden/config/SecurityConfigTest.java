package com.emmagax.eden.config;

import com.emmagax.eden.controller.AuthController;
import com.emmagax.eden.controller.UserController;
import com.emmagax.eden.model.User;
import com.emmagax.eden.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.mockito.ArgumentMatchers.anyString;

@WebMvcTest(controllers = { AuthController.class, UserController.class })
@Import({ SecurityConfig.class, EdenUserDetailsService.class })
class SecurityConfigTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private UserRepository userRepository;

  @Test
  void anonymousUsersCannotAccessProtectedEndpoints() throws Exception {
    mockMvc.perform(get("/users"))
        .andExpect(status().isForbidden());
  }

  @Test
  void anonymousUsersCanRegister() throws Exception {
    when(userRepository.existsByEmail("security-test@example.com")).thenReturn(false);
    when(userRepository.existsByUsername("securitytest")).thenReturn(false);
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
      User user = invocation.getArgument(0);
      user.setId(1L);
      return user;
    });

    mockMvc.perform(post("/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "email": "security-test@example.com",
              "username": "securitytest",
              "password": "Password123!"
            }
            """))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser
  void authenticatedUsersCanAccessProtectedEndpoints() throws Exception {
    when(userRepository.findAll()).thenReturn(List.of());

    mockMvc.perform(get("/users"))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(username = "securitytest")
  void currentUserReturnsAuthenticatedUser() throws Exception {
    User user = new User();
    user.setId(1L);
    user.setEmail("security-test@example.com");
    user.setUsername("securitytest");

    when(userRepository.findByUsername("securitytest")).thenReturn(Optional.of(user));

    mockMvc.perform(get("/auth/me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.email").value("security-test@example.com"))
        .andExpect(jsonPath("$.username").value("securitytest"));
  }

  @Test
  @WithMockUser
  void logoutReturnsNoContent() throws Exception {
    mockMvc.perform(post("/auth/logout"))
        .andExpect(status().isNoContent());
  }

  @Test
  @WithMockUser(username = "securitytest")
  void authenticatedUsersCanRequestEmailVerificationToken() throws Exception {
    User user = new User();
    user.setId(1L);
    user.setEmail("security-test@example.com");
    user.setUsername("securitytest");

    when(userRepository.findByUsername("securitytest")).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    mockMvc.perform(post("/auth/email-verification/request"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isString());
  }

  @Test
  void emailVerificationConfirmMarksEmailVerified() throws Exception {
    User user = new User();
    user.setId(1L);
    user.setEmail("security-test@example.com");
    user.setUsername("securitytest");
    user.setEmailVerificationTokenHash("hashed-token");
    user.setEmailVerificationTokenExpiresAt(LocalDateTime.now().plusHours(1));

    when(userRepository.findByEmailVerificationTokenHash(anyString())).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    mockMvc.perform(post("/auth/email-verification/confirm")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "token": "raw-token"
            }
            """))
        .andExpect(status().isNoContent());
  }

  @Test
  void passwordResetRequestReturnsTokenForExistingAccount() throws Exception {
    User user = new User();
    user.setId(1L);
    user.setEmail("security-test@example.com");
    user.setUsername("securitytest");

    when(userRepository.findByEmail("security-test@example.com")).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    mockMvc.perform(post("/auth/password-reset/request")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "identifier": "security-test@example.com"
            }
            """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isString());
  }

  @Test
  void passwordResetConfirmUpdatesPassword() throws Exception {
    User user = new User();
    user.setId(1L);
    user.setEmail("security-test@example.com");
    user.setUsername("securitytest");
    user.setPassword("old-password-hash");
    user.setPasswordResetTokenHash("hashed-token");
    user.setPasswordResetTokenExpiresAt(LocalDateTime.now().plusHours(1));

    when(userRepository.findByPasswordResetTokenHash(anyString())).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    mockMvc.perform(post("/auth/password-reset/confirm")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "token": "raw-token",
              "newPassword": "NewPassword123!"
            }
            """))
        .andExpect(status().isNoContent());
  }
}
