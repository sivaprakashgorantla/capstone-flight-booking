package com.flight.email_service.service;

import com.flight.email_service.dto.PasswordResetEmailRequest;
import com.flight.email_service.dto.SupportAckEmailRequest;
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

    // ── Support Ticket Acknowledgement ───────────────────────────────────────

    @Async
    public void sendSupportAckEmail(SupportAckEmailRequest request) {
        try {
            String priorityLabel = switch (request.getPriority() != null ? request.getPriority() : "") {
                case "URGENT" -> "🔴 URGENT";
                case "HIGH"   -> "🟠 HIGH";
                case "MEDIUM" -> "🟡 MEDIUM";
                default       -> "🟢 LOW";
            };

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(request.getToEmail());
            message.setSubject("SkyBook Support — Ticket " + request.getTicketReference() + " Received");
            message.setText(
                    "Dear Customer,\n\n" +
                    "Thank you for reaching out to SkyBook Support. Your ticket has been successfully created.\n\n" +
                    "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                    "  Ticket Reference : " + request.getTicketReference() + "\n" +
                    "  Category         : " + formatCategory(request.getCategory()) + "\n" +
                    "  Priority         : " + priorityLabel + "\n" +
                    "  Assigned To      : " + (request.getAssignedTo() != null ? request.getAssignedTo() : "Support Team") + "\n" +
                    "  Subject          : " + (request.getSubject() != null ? request.getSubject() : "—") + "\n" +
                    "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                    "Our support team will review your request and respond within:\n" +
                    "  • URGENT / HIGH  → 2–4 hours\n" +
                    "  • MEDIUM         → 24 hours\n" +
                    "  • LOW            → 48 hours\n\n" +
                    "You can track your ticket status in the SkyBook app under:\n" +
                    "  My Account → Support → My Tickets\n\n" +
                    "Please keep your ticket reference handy for any follow-ups.\n\n" +
                    "Thank you for choosing SkyBook!\n" +
                    "— SkyBook Customer Support Team"
            );
            mailSender.send(message);
            log.info("Support acknowledgement email sent to={} ref={}", request.getToEmail(), request.getTicketReference());
        } catch (Exception e) {
            log.error("Failed to send support-ack email to={} ref={}: {}",
                    request.getToEmail(), request.getTicketReference(), e.getMessage());
        }
    }

    // ── Support Ticket Resolved ───────────────────────────────────────────────

    @Async
    public void sendSupportResolvedEmail(String toEmail, String ticketReference, String resolution) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("SkyBook Support — Ticket " + ticketReference + " Resolved");
            message.setText(
                    "Dear Customer,\n\n" +
                    "We are pleased to inform you that your support ticket has been resolved.\n\n" +
                    "  Ticket Reference : " + ticketReference + "\n" +
                    "  Resolution       :\n\n" +
                    "  " + resolution + "\n\n" +
                    "If you are satisfied with the resolution, no further action is needed — " +
                    "the ticket will be automatically closed in 48 hours.\n\n" +
                    "If you need further assistance, simply reply to this email and we will reopen your ticket.\n\n" +
                    "Thank you for flying with SkyBook!\n" +
                    "— SkyBook Customer Support Team"
            );
            mailSender.send(message);
            log.info("Support resolved email sent to={} ref={}", toEmail, ticketReference);
        } catch (Exception e) {
            log.error("Failed to send support-resolved email to={} ref={}: {}", toEmail, ticketReference, e.getMessage());
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String formatCategory(String category) {
        if (category == null) return "General Enquiry";
        return category.replace("_", " ").toLowerCase()
                .substring(0, 1).toUpperCase() +
                category.replace("_", " ").toLowerCase().substring(1);
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
