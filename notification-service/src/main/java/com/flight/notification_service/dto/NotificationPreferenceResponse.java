package com.flight.notification_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationPreferenceResponse {

    private Long id;
    private String userId;
    private String userEmail;

    // Channel toggles
    private boolean emailEnabled;
    private boolean pushEnabled;
    private boolean smsEnabled;

    // Type toggles
    private boolean bookingConfirmations;
    private boolean delayAlerts;
    private boolean reminders;
    private boolean paymentAlerts;
    private boolean cancellationAlerts;
    private boolean generalAlerts;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String message;
}
