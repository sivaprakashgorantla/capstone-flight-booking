package com.flight.email_service.controller;

import com.flight.email_service.dto.ApiResponse;
import com.flight.email_service.dto.PasswordResetEmailRequest;
import com.flight.email_service.dto.SupportAckEmailRequest;
import com.flight.email_service.dto.WelcomeEmailRequest;
import com.flight.email_service.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/email")
@RequiredArgsConstructor
@Tag(name = "Email Service", description = "Internal email sending endpoints — requires X-Service-Key header")
public class EmailController {

    private final EmailService emailService;

    @Value("${internal.service.key}")
    private String internalServiceKey;

    // ── Health ────────────────────────────────────────────────────────────────

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "No auth required")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("email-service is up and running"));
    }

    // ── Welcome Email ─────────────────────────────────────────────────────────

    @PostMapping("/welcome")
    @Operation(
        summary     = "Send welcome email",
        description = "Triggered on user registration. Requires X-Service-Key header."
    )
    public ResponseEntity<ApiResponse<Void>> sendWelcome(
            @RequestHeader(value = "X-Service-Key", required = false) String serviceKey,
            @Valid @RequestBody WelcomeEmailRequest request) {

        if (!internalServiceKey.equals(serviceKey)) {
            log.warn("Rejected /email/welcome — invalid X-Service-Key");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Invalid service key"));
        }

        log.info("POST /email/welcome — to={}, user={}", request.getToEmail(), request.getUsername());
        emailService.sendWelcomeEmail(request);
        return ResponseEntity.accepted()
                .body(ApiResponse.success("Welcome email queued for " + request.getToEmail()));
    }

    // ── Password Reset Email ──────────────────────────────────────────────────

    @PostMapping("/password-reset")
    @Operation(
        summary     = "Send password reset email",
        description = "Triggered on forgot-password request. Requires X-Service-Key header."
    )
    public ResponseEntity<ApiResponse<Void>> sendPasswordReset(
            @RequestHeader(value = "X-Service-Key", required = false) String serviceKey,
            @Valid @RequestBody PasswordResetEmailRequest request) {

        if (!internalServiceKey.equals(serviceKey)) {
            log.warn("Rejected /email/password-reset — invalid X-Service-Key");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Invalid service key"));
        }

        log.info("POST /email/password-reset — to={}, user={}", request.getToEmail(), request.getUsername());
        emailService.sendPasswordResetEmail(request);
        return ResponseEntity.accepted()
                .body(ApiResponse.success("Password reset email queued for " + request.getToEmail()));
    }

    // ── Support Ticket Acknowledgement ────────────────────────────────────────

    @PostMapping("/support-ack")
    @Operation(
        summary     = "Send support ticket acknowledgement",
        description = "Triggered by support-service when a new ticket is created (UC8 Step 4). Requires X-Service-Key header."
    )
    public ResponseEntity<ApiResponse<Void>> sendSupportAck(
            @RequestHeader(value = "X-Service-Key", required = false) String serviceKey,
            @Valid @RequestBody SupportAckEmailRequest request) {

        if (!internalServiceKey.equals(serviceKey)) {
            log.warn("Rejected /email/support-ack — invalid X-Service-Key");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Invalid service key"));
        }

        log.info("POST /email/support-ack — to={} ref={}", request.getToEmail(), request.getTicketReference());
        emailService.sendSupportAckEmail(request);
        return ResponseEntity.accepted()
                .body(ApiResponse.success("Support acknowledgement email queued for " + request.getToEmail()));
    }

    // ── Support Ticket Resolved ───────────────────────────────────────────────

    @PostMapping("/support-resolved")
    @Operation(
        summary     = "Send support ticket resolved email",
        description = "Triggered by support-service when a ticket is resolved. Requires X-Service-Key header."
    )
    public ResponseEntity<ApiResponse<Void>> sendSupportResolved(
            @RequestHeader(value = "X-Service-Key", required = false) String serviceKey,
            @RequestBody java.util.Map<String, String> body) {

        if (!internalServiceKey.equals(serviceKey)) {
            log.warn("Rejected /email/support-resolved — invalid X-Service-Key");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Invalid service key"));
        }

        String toEmail   = body.getOrDefault("toEmail", "");
        String ticketRef = body.getOrDefault("ticketReference", "");
        String resolution = body.getOrDefault("resolution", "");

        log.info("POST /email/support-resolved — to={} ref={}", toEmail, ticketRef);
        emailService.sendSupportResolvedEmail(toEmail, ticketRef, resolution);
        return ResponseEntity.accepted()
                .body(ApiResponse.success("Resolution email queued for " + toEmail));
    }
}
