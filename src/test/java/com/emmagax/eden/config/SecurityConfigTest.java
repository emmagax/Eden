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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
}
