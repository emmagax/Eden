package com.emmagax.eden.controller;

import com.emmagax.eden.model.User;
import com.emmagax.eden.repository.UserRepository;
import org.springframework.web.bind.annotation.*;
import com.emmagax.eden.dto.UpdateUserRequest;
import com.emmagax.eden.dto.UserResponse;
import jakarta.validation.Valid;
import com.emmagax.eden.config.AccountAccess;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/users")
public class UserController {

  private final UserRepository userRepository;
  private final AccountAccess access;

  public UserController(UserRepository userRepository, AccountAccess access) {
    this.userRepository = userRepository;
    this.access = access;
  }

  @PutMapping("/{userId}")
  public UserResponse update(
      @PathVariable Long userId,
      @Valid @RequestBody UpdateUserRequest request, Authentication authentication) {
    User user = access.current(authentication);
    if (!user.getId().equals(userId))
      throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
    // Session principals use username; changing it requires a new sign-in.
    if (!user.getUsername().equals(request.username()))
      throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Username changes are not supported yet");
    if (!user.getEmail().equals(request.email())) user.setEmailVerified(false);

    user.setEmail(request.email());
    user.setUsername(request.username());

    User savedUser = userRepository.save(user);
    return toUserResponse(savedUser);

  }

  private UserResponse toUserResponse(User user) {
    return new UserResponse(user.getId(), user.getEmail(), user.getUsername());
  }
}
