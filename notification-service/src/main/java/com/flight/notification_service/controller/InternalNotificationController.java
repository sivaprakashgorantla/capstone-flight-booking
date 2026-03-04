package com.flight.notification_service.controller;

import com.flight.notification_service.dto.*;
import com.flight.notification_service.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Internal REST endpoints called by sibling microservices
 * (booking-service, payment-service, cancellation-service, flight-service).
 *
 * Security: X-Service-Key header is validated on every request.
 * These endpoints are public in SecurityConfig (no JWT) — the service
 * key acts as the shared secret.
 */
@Slf4j
@RestController
@RequestMapping("/notifications/internal")
@RequiredArgsConstructor
@Tag(name = "Internal Notifications",
        description = "Service-to-service endpoints (X-Service-Key required). " +
                "Called by booking, payment, cancellation and flight services.")
public class InternalNotificationController {

    private final NotificationService notificationService;

    @Value("${internal.service.key}")
    private String expectedServiceKey;

    // ─────────────────────────────────────────────────────────────────────────
    // UC9 Step 2a — Booking Confirmation  (called by booking-service)
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/booking-confirmation")
    @Operation(
            summary     = "Send booking confirmation (UC9 Step 2a)",
            description = "Called by booking-service after a booking is confirmed. " +
                    "Sends BOOKING_CONFIRMATION notification to the user.",
            security    = @SecurityRequirement(name = "serviceKey")
    )
    public ResponseEntity<ApiResponse<NotificationResponse>> bookingConfirmation(
            @Valid @RequestBody BookingNotificationRequest request,
            HttpServletRequest httpRequest) {

        validateServiceKey(httpRequest);
        request.setType("BOOKING_CONFIRMATION");
        log.info("[INTERNAL] booking-confirmation for userId={} ref={}",
                request.getUserId(), request.getBookingReference());
        NotificationResponse resp = notificationService.sendBookingNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking confirmation notification dispatched", resp));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Payment Success  (called by payment-service)
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/payment-success")
    @Operation(
            summary     = "Send payment success notification",
            description = "Called by payment-service after payment is processed. " +
                    "Sends PAYMENT_SUCCESS notification to the user.",
            security    = @SecurityRequirement(name = "serviceKey")
    )
    public ResponseEntity<ApiResponse<NotificationResponse>> paymentSuccess(
            @Valid @RequestBody BookingNotificationRequest request,
            HttpServletRequest httpRequest) {

        validateServiceKey(httpRequest);
        request.setType("PAYMENT_SUCCESS");
        log.info("[INTERNAL] payment-success for userId={} ref={}",
                request.getUserId(), request.getBookingReference());
        NotificationResponse resp = notificationService.sendBookingNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment success notification dispatched", resp));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UC9 Step 2b — Delay Alert  (called by flight-service or admin)
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/delay-alert")
    @Operation(
            summary     = "Send flight delay alert (UC9 Step 2b)",
            description = "Called by flight-service when a flight departure is delayed. " +
                    "Sends DELAY_ALERT notification to the affected passenger.",
            security    = @SecurityRequirement(name = "serviceKey")
    )
    public ResponseEntity<ApiResponse<NotificationResponse>> delayAlert(
            @Valid @RequestBody DelayAlertRequest request,
            HttpServletRequest httpRequest) {

        validateServiceKey(httpRequest);
        log.info("[INTERNAL] delay-alert for userId={} flight={} delay={}min",
                request.getUserId(), request.getFlightNumber(), request.getDelayMinutes());
        NotificationResponse resp = notificationService.sendDelayAlert(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Delay alert dispatched", resp));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UC9 Step 2c — Pre-departure Reminder  (called by scheduler / admin)
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/reminder")
    @Operation(
            summary     = "Send pre-departure reminder (UC9 Step 2c)",
            description = "Sends a FLIGHT_REMINDER notification (e.g. 24h or 2h before departure). " +
                    "Typically triggered by a scheduled job.",
            security    = @SecurityRequirement(name = "serviceKey")
    )
    public ResponseEntity<ApiResponse<NotificationResponse>> reminder(
            @Valid @RequestBody ReminderRequest request,
            HttpServletRequest httpRequest) {

        validateServiceKey(httpRequest);
        log.info("[INTERNAL] reminder for userId={} flight={} {}h before",
                request.getUserId(), request.getFlightNumber(), request.getHoursBeforeDeparture());
        NotificationResponse resp = notificationService.sendReminder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Reminder dispatched", resp));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cancellation Confirmation  (called by cancellation-service)
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/cancellation-confirmation")
    @Operation(
            summary     = "Send cancellation confirmation",
            description = "Called by cancellation-service after a booking is cancelled. " +
                    "Sends CANCELLATION_CONFIRMATION with refund details.",
            security    = @SecurityRequirement(name = "serviceKey")
    )
    public ResponseEntity<ApiResponse<NotificationResponse>> cancellationConfirmation(
            @Valid @RequestBody CancellationNotificationRequest request,
            HttpServletRequest httpRequest) {

        validateServiceKey(httpRequest);
        log.info("[INTERNAL] cancellation-confirmation for userId={} ref={}",
                request.getUserId(), request.getBookingReference());
        NotificationResponse resp = notificationService.sendCancellationNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Cancellation notification dispatched", resp));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // General Alert  (admin broadcast via service key)
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/general-alert")
    @Operation(
            summary     = "Send general alert to a user",
            description = "Admin-initiated general broadcast notification. " +
                    "Requires X-Service-Key header.",
            security    = @SecurityRequirement(name = "serviceKey")
    )
    public ResponseEntity<ApiResponse<NotificationResponse>> generalAlert(
            @Valid @RequestBody GeneralAlertRequest request,
            HttpServletRequest httpRequest) {

        validateServiceKey(httpRequest);
        log.info("[INTERNAL] general-alert for userId={} title={}",
                request.getUserId(), request.getTitle());
        NotificationResponse resp = notificationService.sendGeneralAlert(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("General alert dispatched", resp));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Service Key validation
    // ─────────────────────────────────────────────────────────────────────────

    private void validateServiceKey(HttpServletRequest request) {
        String key = request.getHeader("X-Service-Key");
        if (key == null || !key.equals(expectedServiceKey)) {
            log.warn("[INTERNAL] Invalid or missing X-Service-Key from {}",
                    request.getRemoteAddr());
            throw new com.flight.notification_service.exception.BadRequestException(
                    "Invalid or missing X-Service-Key header");
        }
    }
}
