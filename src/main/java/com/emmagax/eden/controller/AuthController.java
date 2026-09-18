package com.emmagax.eden.controller;

import com.emmagax.eden.dto.AuthUserResponse;
import com.emmagax.eden.dto.LoginRequest;
import com.emmagax.eden.dto.RegisterRequest;
import com.emmagax.eden.dto.RegisterResponse;
import com.emmagax.eden.dto.AuthTokenResponse;
import com.emmagax.eden.dto.ConfirmEmailVerificationRequest;
import com.emmagax.eden.dto.ConfirmPasswordResetRequest;
import com.emmagax.eden.dto.PasswordResetRequest;
import com.emmagax.eden.exception.DuplicateAccountFieldException;
import com.emmagax.eden.model.User;
import com.emmagax.eden.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class AuthController {

  private final AuthenticationManager authenticationManager;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public AuthController(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      AuthenticationManager authenticationManager) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.authenticationManager = authenticationManager;
  }

  @PostMapping("/register")
  public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
    if (userRepository.existsByEmail(request.email())) {
      throw new DuplicateAccountFieldException(
          "EMAIL_ALREADY_EXISTS",
          "email",
          "An account with this email already exists");
    }

    if (userRepository.existsByUsername(request.username())) {
      throw new DuplicateAccountFieldException(
          "USERNAME_ALREADY_EXISTS",
          "username",
          "This username is already taken");
    }

    User user = new User();
    user.setEmail(request.email());
    user.setUsername(request.username());
    user.setPassword(passwordEncoder.encode(request.password()));

    User savedUser = userRepository.save(user);
    return new RegisterResponse(savedUser.getId(), savedUser.getEmail(), savedUser.getUsername());
  }

  @PostMapping("/login")
  public ResponseEntity<AuthUserResponse> login(
      @Valid @RequestBody LoginRequest loginRequest,
      HttpServletRequest request) {
    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            loginRequest.getIdentifier(),
            loginRequest.getPassword()));

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);

    request.getSession(true).setAttribute(
        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
        context);

    User user = userRepository.findByEmail(loginRequest.getIdentifier())
        .or(() -> userRepository.findByUsername(loginRequest.getIdentifier()))
        .orElseThrow();

    return ResponseEntity.ok(
        new AuthUserResponse(user.getId(), user.getEmail(), user.getUsername()));
  }

  @GetMapping("/me")
  public AuthUserResponse me(Authentication authentication) {
    User user = userRepository.findByUsername(authentication.getName()).orElseThrow();

    return new AuthUserResponse(user.getId(), user.getEmail(), user.getUsername());
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(HttpServletRequest request) {
    HttpSession session = request.getSession(false);

    if (session != null) {
      session.invalidate();
    }

    SecurityContextHolder.clearContext();

    return ResponseEntity.noContent().build();
  }

  @PostMapping("/email-verification/request")
  public AuthTokenResponse requestEmailVerification(Authentication authentication) {
    User user = userRepository.findByUsername(authentication.getName()).orElseThrow();

    String token = generateToken();

    user.setEmailVerificationTokenHash(hashToken(token));
    user.setEmailVerificationTokenExpiresAt(LocalDateTime.now().plusHours(24));

    userRepository.save(user);

    return new AuthTokenResponse(token);
  }

  @PostMapping("/email-verification/confirm")
  public ResponseEntity<Void> confirmEmailVerification(
      @Valid @RequestBody ConfirmEmailVerificationRequest request) {
    String tokenHash = hashToken(request.token());

    User user = userRepository.findByEmailVerificationTokenHash(tokenHash)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired token"));

    if (isExpired(user.getEmailVerificationTokenExpiresAt())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired token");
    }

    user.setEmailVerified(true);
    user.setEmailVerificationTokenHash(null);
    user.setEmailVerificationTokenExpiresAt(null);

    userRepository.save(user);

    return ResponseEntity.noContent().build();
  }

  @PostMapping("/password-reset/request")
  public AuthTokenResponse requestPasswordReset(
      @Valid @RequestBody PasswordResetRequest request) {
    User user = userRepository.findByEmail(request.identifier())
        .or(() -> userRepository.findByUsername(request.identifier()))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid account"));

    String token = generateToken();

    user.setPasswordResetTokenHash(hashToken(token));
    user.setPasswordResetTokenExpiresAt(LocalDateTime.now().plusHours(1));

    userRepository.save(user);

    return new AuthTokenResponse(token);
  }

  @PostMapping("/password-reset/confirm")
  public ResponseEntity<Void> confirmPasswordReset(
      @Valid @RequestBody ConfirmPasswordResetRequest request) {
    String tokenHash = hashToken(request.token());

    User user = userRepository.findByPasswordResetTokenHash(tokenHash)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired token"));

    if (isExpired(user.getPasswordResetTokenExpiresAt())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired token");
    }

    user.setPassword(passwordEncoder.encode(request.newPassword()));
    user.setPasswordResetTokenHash(null);
    user.setPasswordResetTokenExpiresAt(null);

    userRepository.save(user);

    return ResponseEntity.noContent().build();
  }

  private String generateToken() {
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String hashToken(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(token.getBytes());
      return Base64.getEncoder().encodeToString(hash);
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to hash token", exception);
    }
  }

  private boolean isExpired(LocalDateTime expiresAt) {
    return expiresAt == null || expiresAt.isBefore(LocalDateTime.now());
  }
}
