package com.emmagax.eden.controller;

import com.emmagax.eden.dto.LoginRequest;
import com.emmagax.eden.dto.RegisterRequest;
import com.emmagax.eden.dto.RegisterResponse;
import com.emmagax.eden.exception.DuplicateAccountFieldException;
import com.emmagax.eden.model.User;
import com.emmagax.eden.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateAccountFieldException(
                    "EMAIL_ALREADY_EXISTS",
                    "email",
                    "An account with this email already exists"
            );
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateAccountFieldException(
                    "USERNAME_ALREADY_EXISTS",
                    "username",
                    "This username is already taken"
            );
        }

        User user = new User();

        user.setEmail(request.email());
        user.setUsername(request.username());

        String hashedPassword = passwordEncoder.encode(request.password());
        user.setPassword(hashedPassword);

        User savedUser = userRepository.save(user);
        return new RegisterResponse(savedUser.getId(), savedUser.getEmail(), savedUser.getUsername());
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {

        Optional<User> maybeUser = userRepository.findByEmail(loginRequest.getIdentifier());

        if (maybeUser.isEmpty()) {
            maybeUser = userRepository.findByUsername(loginRequest.getIdentifier());
        }

        if (maybeUser.isEmpty()) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        User user = maybeUser.get();

        if (user.getPassword() == null) {
            return ResponseEntity.status(401).body("This account uses Google sign-in");
        }

        boolean passwordMatches = passwordEncoder.matches(loginRequest.getPassword(), user.getPassword());

        if (!passwordMatches) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        return ResponseEntity.ok("Login successful for " + user.getUsername());
    }
}
