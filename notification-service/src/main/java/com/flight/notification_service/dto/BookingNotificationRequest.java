package com.flight.notification_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payload sent by booking-service or payment-service to trigger
 * a BOOKING_CONFIRMATION or PAYMENT_SUCCESS notification.
 * Header: X-Service-Key required.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Internal request from booking/payment-service to send a booking notification")
public class BookingNotificationRequest {

    @NotBlank(message = "userId is required")
    @Schema(example = "user-123")
    private String userId;

    @NotBlank @Email
    @Schema(example = "john@example.com")
    private String userEmail;

    @NotBlank
    @Schema(example = "BKG-A1B2C3D4")
    private String bookingReference;

    @Schema(example = "AI-202")
    private String flightNumber;

    @Schema(example = "Delhi")
    private String departureCity;

    @Schema(example = "Mumbai")
    private String destinationCity;

    @Schema(description = "Scheduled departure time")
    private LocalDateTime departureTime;

    @Schema(example = "2")
    private int passengerCount;

    @Schema(example = "12500.00")
    private BigDecimal totalAmount;

    /**
     * BOOKING_CONFIRMATION or PAYMENT_SUCCESS
     */
    @NotNull
    @Schema(example = "BOOKING_CONFIRMATION",
            allowableValues = {"BOOKING_CONFIRMATION", "PAYMENT_SUCCESS"})
    private String type;
}
