package com.auth_service.flight.demo.service;

import com.auth_service.flight.demo.dto.AuthRequest;
import com.auth_service.flight.demo.dto.AuthResponse;
import com.auth_service.flight.demo.dto.RegisterRequest;
import com.auth_service.flight.demo.model.User;
import com.auth_service.flight.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository        userRepository;
    private final PasswordEncoder       passwordEncoder;
    private final JwtService            jwtService;
    private final AuthenticationManager authenticationManager;

    // ─── Register ──────────────────────────────────────────────────────────────

    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration failed — username already taken: {}", request.getUsername());
            throw new IllegalArgumentException("Username already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed — email already in use: {}", request.getEmail());
            throw new IllegalArgumentException("Email already in use: " + request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(request.getRole())
                .active(true)
                .build();

        userRepository.save(user);
        log.info("User registered: {} ({})", user.getUsername(), user.getEmail());

        return buildResponse(jwtService.generateToken(user), user);
    }

    // ─── Login ─────────────────────────────────────────────────────────────────

    public AuthResponse login(AuthRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        log.info("Login successful for user: {}", user.getUsername());
        return buildResponse(jwtService.generateToken(user), user);
    }

    // ─── Validate ──────────────────────────────────────────────────────────────

    public boolean validateToken(String token) {
        try {
            String username = jwtService.extractUsername(token);
            User user = userRepository.findByUsername(username).orElse(null);
            if (user == null) {
                log.warn("Token validation failed — user not found");
                return false;
            }
            boolean valid = jwtService.isTokenValid(token, user);
            log.debug("Token validation for '{}': {}", username, valid);
            return valid;
        } catch (Exception e) {
            log.warn("Token validation error: {}", e.getMessage());
            return false;
        }
    }

    // ─── Helper ────────────────────────────────────────────────────────────────

    private AuthResponse buildResponse(String token, User user) {
        return AuthResponse.builder()
                .token(token)
                .expiresIn(jwtService.getExpirationMs())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
