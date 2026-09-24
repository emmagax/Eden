package com.emmagax.eden.controller;

import com.emmagax.eden.model.User;
import com.emmagax.eden.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.emmagax.eden.dto.UpdateUserRequest;
import com.emmagax.eden.dto.UserResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/users")
public class UserController {

  private final UserRepository userRepository;

  public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
  }

  @PutMapping("/{userId}")
  public UserResponse update(
      @PathVariable Long userId,
      @Valid @RequestBody UpdateUserRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("User not found"));

    user.setEmail(request.email());
    user.setUsername(request.username());

    User savedUser = userRepository.save(user);
    return toUserResponse(savedUser);

  }

  private UserResponse toUserResponse(User user) {
    return new UserResponse(user.getId(), user.getEmail(), user.getUsername());
  }
}
