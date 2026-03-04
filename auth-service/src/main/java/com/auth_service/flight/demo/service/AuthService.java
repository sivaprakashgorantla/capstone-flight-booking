package com.auth_service.flight.demo.service;

import com.auth_service.flight.demo.dto.AuthRequest;
import com.auth_service.flight.demo.dto.AuthResponse;
import com.auth_service.flight.demo.dto.ForgotPasswordRequest;
import com.auth_service.flight.demo.dto.RegisterRequest;
import com.auth_service.flight.demo.dto.ResetPasswordRequest;
import com.auth_service.flight.demo.model.PasswordResetToken;
import com.auth_service.flight.demo.model.User;
import com.auth_service.flight.demo.repository.PasswordResetTokenRepository;
import com.auth_service.flight.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository               userRepository;
    private final PasswordEncoder              passwordEncoder;
    private final JwtService                   jwtService;
    private final AuthenticationManager        authenticationManager;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final EmailService                 emailService;

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

    // ─── Forgot Password ───────────────────────────────────────────────────────

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        log.info("Forgot password request for email: {}", request.getEmail());

        // Always respond with success to avoid email enumeration
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            // Invalidate any previous tokens for this user
            resetTokenRepository.deleteAllByUser(user);

            String rawToken = UUID.randomUUID().toString();
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token(rawToken)
                    .user(user)
                    .expiresAt(LocalDateTime.now().plusMinutes(30))
                    .used(false)
                    .build();

            resetTokenRepository.save(resetToken);
            emailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), rawToken);
            log.info("Password reset token issued for user: {}", user.getUsername());
        });
    }

    // ─── Reset Password ────────────────────────────────────────────────────────

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        PasswordResetToken resetToken = resetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token"));

        if (resetToken.isUsed()) {
            throw new IllegalArgumentException("Reset token has already been used");
        }
        if (resetToken.isExpired()) {
            throw new IllegalArgumentException("Reset token has expired. Please request a new one");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        resetTokenRepository.save(resetToken);

        log.info("Password successfully reset for user: {}", user.getUsername());
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
