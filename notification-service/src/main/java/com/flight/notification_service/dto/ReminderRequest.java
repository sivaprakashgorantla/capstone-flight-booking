package com.flight.notification_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Payload to trigger a pre-departure FLIGHT_REMINDER notification (UC9 Step 2c).
 * Typically called by a scheduler or admin.
 * Header: X-Service-Key required.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Internal request to send a flight reminder notification")
public class ReminderRequest {

    @NotBlank
    @Schema(example = "user-123")
    private String userId;

    @NotBlank @Email
    @Schema(example = "john@example.com")
    private String userEmail;

    @NotBlank
    @Schema(example = "BKG-A1B2C3D4")
    private String bookingReference;

    @NotBlank
    @Schema(example = "AI-202")
    private String flightNumber;

    @Schema(example = "Delhi")
    private String departureCity;

    @Schema(example = "Mumbai")
    private String destinationCity;

    @Schema(description = "Scheduled departure time")
    private LocalDateTime departureTime;

    @Schema(example = "24", description = "Hours before departure this reminder is for (e.g. 24 or 2)")
    private int hoursBeforeDeparture;
}
