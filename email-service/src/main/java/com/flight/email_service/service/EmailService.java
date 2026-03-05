package com.flight.email_service.service;

import com.flight.email_service.dto.PasswordResetEmailRequest;
import com.flight.email_service.dto.WelcomeEmailRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${app.reset-password.url:http://localhost:3000/reset-password}")
    private String resetPasswordBaseUrl;

    @Value("${app.login.url:http://localhost:3000/login}")
    private String loginUrl;

    // ── Welcome Email ─────────────────────────────────────────────────────────

    @Async
    public void sendWelcomeEmail(WelcomeEmailRequest request) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(request.getToEmail());
            message.setSubject("Welcome to SkyBook ✈ — Registration Successful");
            message.setText(
                    "Hi " + request.getUsername() + ",\n\n" +
                    "Welcome aboard! Your SkyBook account has been created successfully.\n\n" +
                    "You can now log in and start searching for flights:\n" +
                    loginUrl + "\n\n" +
                    "Account Details:\n" +
                    "  Username : " + request.getUsername() + "\n" +
                    "  Email    : " + request.getToEmail() + "\n\n" +
                    "If you did not create this account, please contact our support team immediately.\n\n" +
                    "Happy travels!\n" +
                    "— The SkyBook Team"
            );
            mailSender.send(message);
            log.info("Welcome email sent to: {}", request.getToEmail());
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", request.getToEmail(), e.getMessage());
        }
    }

    // ── Password Reset Email ──────────────────────────────────────────────────

    @Async
    public void sendPasswordResetEmail(PasswordResetEmailRequest request) {
        try {
            String resetLink = resetPasswordBaseUrl + "?token=" + request.getResetToken();

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(request.getToEmail());
            message.setSubject("SkyBook — Password Reset Request");
            message.setText(
                    "Hi " + request.getUsername() + ",\n\n" +
                    "We received a request to reset your SkyBook account password.\n\n" +
                    "Click the link below to reset your password (valid for 30 minutes):\n\n" +
                    resetLink + "\n\n" +
                    "If you did not request this, please ignore this email — your account is safe.\n\n" +
                    "— The SkyBook Team"
            );
            mailSender.send(message);
            log.info("Password reset email sent to: {}", request.getToEmail());
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", request.getToEmail(), e.getMessage());
        }
    }
}
