package com.emmagax.coro.controller;

import com.emmagax.coro.model.User;
import com.emmagax.coro.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<User> getAll() {
        return userRepository.findAll();
    }

    @PutMapping("/{userId}")
    public User update(
            @PathVariable Long userId,
            @RequestBody User updates
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setEmail(updates.getEmail());
        user.setUsername(updates.getUsername());

        return userRepository.save(user);
    }
}