package com.emmagax.eden.controller;

import com.emmagax.eden.config.SecurityConfig;
import com.emmagax.eden.model.Profile;
import com.emmagax.eden.model.User;
import com.emmagax.eden.repository.ProfileRepository;
import com.emmagax.eden.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProfileController.class)
@Import(SecurityConfig.class)
class ProfileControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ProfileRepository profileRepository;

  @MockitoBean
  private UserRepository userRepository;

  @Test
  @WithMockUser
  void createProfileReturnsExpandedProfileFields() throws Exception {
    User user = new User();
    user.setId(1L);
    user.setUsername("emmatest");

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> {
      Profile profile = invocation.getArgument(0);
      profile.setId(2L);
      return profile;
    });

    mockMvc.perform(post("/profiles/users/1/profile")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "artistName": "Emma G",
              "handle": "emma_g",
              "pronouns": "she/her",
              "zone": "Chicago",
              "bio": "Independent artist",
              "roles": "artist,producer",
              "genres": "indie,electronic",
              "scene": "Chicago DIY",
              "avatarUrl": "https://example.com/avatar.png"
            }
            """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(2))
        .andExpect(jsonPath("$.artistName").value("Emma G"))
        .andExpect(jsonPath("$.handle").value("emma_g"))
        .andExpect(jsonPath("$.roles").value("artist,producer"))
        .andExpect(jsonPath("$.genres").value("indie,electronic"))
        .andExpect(jsonPath("$.scene").value("Chicago DIY"))
        .andExpect(jsonPath("$.avatarUrl").value("https://example.com/avatar.png"))
        .andExpect(jsonPath("$.onboardingComplete").value(false))
        .andExpect(jsonPath("$.user.id").value(1))
        .andExpect(jsonPath("$.user.username").value("emmatest"));
  }
}
