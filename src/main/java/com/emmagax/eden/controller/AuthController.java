package com.emmagax.eden.controller;

import com.emmagax.eden.dto.AuthUserResponse;
import com.emmagax.eden.dto.LoginRequest;
import com.emmagax.eden.dto.RegisterRequest;
import com.emmagax.eden.dto.RegisterResponse;
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

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager
    ) {
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
        user.setPassword(passwordEncoder.encode(request.password()));

        User savedUser = userRepository.save(user);
        return new RegisterResponse(savedUser.getId(), savedUser.getEmail(), savedUser.getUsername());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthUserResponse> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request
    ) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getIdentifier(),
                        loginRequest.getPassword()
                )
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        request.getSession(true).setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                context
        );

        User user = userRepository.findByEmail(loginRequest.getIdentifier())
                .or(() -> userRepository.findByUsername(loginRequest.getIdentifier()))
                .orElseThrow();

        return ResponseEntity.ok(
                new AuthUserResponse(user.getId(), user.getEmail(), user.getUsername())
        );
    }
}
