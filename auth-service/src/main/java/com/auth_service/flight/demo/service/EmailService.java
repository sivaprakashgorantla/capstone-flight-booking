package com.auth_service.flight.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Auth-service email client.
 *
 * All actual SMTP logic now lives in the dedicated email-service (port 8086).
 * This class delegates via @LoadBalanced RestTemplate → lb://EMAIL-SERVICE.
 * Calls are @Async so they never block the HTTP response.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final RestTemplate restTemplate; // @LoadBalanced bean from AppConfig

    @Value("${email.service.url:http://EMAIL-SERVICE}")
    private String emailServiceUrl;

    @Value("${internal.service.key}")
    private String serviceKey;

    // ── Welcome Email ─────────────────────────────────────────────────────────

    @Async
    public void sendWelcomeEmail(String toEmail, String username) {
        String url = emailServiceUrl + "/email/welcome";
        try {
            HttpHeaders headers = buildHeaders();
            Map<String, String> body = Map.of(
                    "toEmail",  toEmail,
                    "username", username
            );
            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
            log.info("Welcome email delegated to email-service — to={}, status={}",
                    toEmail, response.getStatusCode());
        } catch (Exception e) {
            log.error("Failed to delegate welcome email to email-service for {}: {}",
                    toEmail, e.getMessage());
        }
    }

    // ── Password Reset Email ──────────────────────────────────────────────────

    @Async
    public void sendPasswordResetEmail(String toEmail, String username, String token) {
        String url = emailServiceUrl + "/email/password-reset";
        try {
            HttpHeaders headers = buildHeaders();
            Map<String, String> body = Map.of(
                    "toEmail",    toEmail,
                    "username",   username,
                    "resetToken", token
            );
            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
            log.info("Password reset email delegated to email-service — to={}, status={}",
                    toEmail, response.getStatusCode());
        } catch (Exception e) {
            log.error("Failed to delegate password reset email to email-service for {}: {}",
                    toEmail, e.getMessage());
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Service-Key", serviceKey);
        return headers;
    }
}
