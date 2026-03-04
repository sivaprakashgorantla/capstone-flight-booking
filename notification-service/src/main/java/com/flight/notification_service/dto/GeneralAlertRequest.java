package com.flight.notification_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Admin-initiated general broadcast to a specific user.
 * Header: X-Service-Key OR Admin JWT required.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Admin request to send a general alert to a user")
public class GeneralAlertRequest {

    @NotBlank
    @Schema(example = "user-123")
    private String userId;

    @NotBlank @Email
    @Schema(example = "john@example.com")
    private String userEmail;

    @NotBlank
    @Schema(example = "Scheduled Maintenance on 10-Mar")
    private String title;

    @NotBlank
    @Schema(example = "Our platform will be down for maintenance from 02:00–04:00 IST on 10-Mar.")
    private String message;
}
