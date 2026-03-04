package com.auth_service.flight.demo.service;

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

    @Async
    public void sendWelcomeEmail(String toEmail, String username) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("Welcome to SkyBook ✈ — Registration Successful");
            message.setText(
                    "Hi " + username + ",\n\n" +
                    "Welcome aboard! Your SkyBook account has been created successfully.\n\n" +
                    "You can now log in and start searching for flights:\n" +
                    "http://localhost:3000/login\n\n" +
                    "Account Details:\n" +
                    "  Username : " + username + "\n" +
                    "  Email    : " + toEmail + "\n\n" +
                    "If you did not create this account, please contact our support team immediately.\n\n" +
                    "Happy travels!\n" +
                    "— The SkyBook Team"
            );
            mailSender.send(message);
            log.info("Welcome email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String username, String token) {
        String resetLink = resetPasswordBaseUrl + "?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("SkyBook — Password Reset Request");
        message.setText(
                "Hi " + username + ",\n\n" +
                "We received a request to reset your SkyBook account password.\n\n" +
                "Click the link below to reset your password (valid for 30 minutes):\n\n" +
                resetLink + "\n\n" +
                "If you did not request this, please ignore this email.\n\n" +
                "— The SkyBook Team"
        );

        mailSender.send(message);
        log.info("Password reset email sent to: {}", toEmail);
    }
}
