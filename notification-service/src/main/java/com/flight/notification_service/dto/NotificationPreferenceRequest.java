package com.flight.notification_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "UC9 Step 1 & 3 — Subscribe to / update notification preferences")
public class NotificationPreferenceRequest {

    // ── Channel toggles ───────────────────────────────────────────────────────

    @Builder.Default
    @Schema(description = "Receive notifications via Email", example = "true")
    private boolean emailEnabled = true;

    @Builder.Default
    @Schema(description = "Receive push notifications", example = "true")
    private boolean pushEnabled = true;

    @Builder.Default
    @Schema(description = "Receive SMS notifications", example = "false")
    private boolean smsEnabled = false;

    // ── Type toggles ──────────────────────────────────────────────────────────

    @Builder.Default
    @Schema(description = "Booking confirmation alerts (UC9 Step 2a)", example = "true")
    private boolean bookingConfirmations = true;

    @Builder.Default
    @Schema(description = "Flight delay alerts (UC9 Step 2b)", example = "true")
    private boolean delayAlerts = true;

    @Builder.Default
    @Schema(description = "Pre-departure reminders (UC9 Step 2c)", example = "true")
    private boolean reminders = true;

    @Builder.Default
    @Schema(description = "Payment success/failure alerts", example = "true")
    private boolean paymentAlerts = true;

    @Builder.Default
    @Schema(description = "Cancellation confirmation alerts", example = "true")
    private boolean cancellationAlerts = true;

    @Builder.Default
    @Schema(description = "General system broadcast alerts", example = "true")
    private boolean generalAlerts = true;
}
