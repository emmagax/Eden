package com.emmagax.coro.controller;

import com.emmagax.coro.dto.LoginRequest;
import com.emmagax.coro.model.User;
import com.emmagax.coro.repository.UserRepository;
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
    public User register(@RequestBody User user) {
        String hashedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(hashedPassword);
        return userRepository.save(user);
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