package com.flight.notification_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Payload sent by flight-service (or admin) to trigger a DELAY_ALERT
 * for all passengers on an affected flight.
 * Header: X-Service-Key required.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Internal request to dispatch a flight delay alert")
public class DelayAlertRequest {

    @NotBlank(message = "userId is required")
    @Schema(example = "user-123")
    private String userId;

    @NotBlank @Email
    @Schema(example = "john@example.com")
    private String userEmail;

    @Schema(example = "BKG-A1B2C3D4")
    private String bookingReference;

    @NotBlank
    @Schema(example = "AI-202")
    private String flightNumber;

    @Schema(description = "Original scheduled departure")
    private LocalDateTime originalDeparture;

    @Schema(description = "Revised departure time after delay")
    private LocalDateTime newDeparture;

    @Min(value = 1, message = "Delay must be at least 1 minute")
    @Schema(example = "120", description = "Delay in minutes")
    private int delayMinutes;

    @Schema(example = "Air traffic congestion")
    private String reason;
}
