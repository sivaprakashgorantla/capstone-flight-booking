package com.flight.notification_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;

/**
 * Payload sent by cancellation-service to trigger a CANCELLATION_CONFIRMATION.
 * Header: X-Service-Key required.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Internal request from cancellation-service")
public class CancellationNotificationRequest {

    @NotBlank
    @Schema(example = "user-123")
    private String userId;

    @NotBlank @Email
    @Schema(example = "john@example.com")
    private String userEmail;

    @NotBlank
    @Schema(example = "BKG-A1B2C3D4")
    private String bookingReference;

    @Schema(example = "CAN-XY123456")
    private String cancellationReference;

    @Schema(example = "AI-202")
    private String flightNumber;

    @Schema(example = "12500.00")
    private BigDecimal originalAmount;

    @Schema(example = "9375.00")
    private BigDecimal refundAmount;

    @Schema(example = "75")
    private int refundPercentage;

    @Schema(example = "TXN-REF-0099")
    private String refundTransactionId;
}
