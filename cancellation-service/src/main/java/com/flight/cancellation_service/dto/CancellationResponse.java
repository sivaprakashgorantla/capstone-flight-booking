package com.flight.cancellation_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Result of a cancellation request")
public class CancellationResponse {

    @Schema(description = "Cancellation record ID", example = "1")
    private Long id;

    @Schema(description = "Unique cancellation reference", example = "CAN-A1B2C3D4")
    private String cancellationReference;

    @Schema(description = "Booking ID that was cancelled", example = "5")
    private Long bookingId;

    @Schema(description = "Booking reference that was cancelled", example = "BKG-XYZ98765")
    private String bookingReference;

    @Schema(description = "User who initiated the cancellation", example = "john")
    private String userId;

    @Schema(description = "User email for notification", example = "john@flight.com")
    private String userEmail;

    private String flightNumber;
    private String airline;
    private String departureCity;
    private String destinationCity;
    private LocalDateTime departureTime;

    @Schema(description = "Original booking amount", example = "9999.00")
    private BigDecimal originalAmount;

    @Schema(description = "Refund percentage applied", example = "75")
    private int refundPercentage;

    @Schema(description = "Refund amount to be credited", example = "7499.25")
    private BigDecimal refundAmount;

    @Schema(description = "Reason provided by the user", example = "Change of travel plans")
    private String cancellationReason;

    @Schema(description = "Cancellation status", example = "REFUNDED")
    private String status;

    @Schema(description = "Simulated refund transaction ID", example = "REF-TXN-1234567890")
    private String refundTransactionId;

    @Schema(description = "Hours before departure when cancellation was made", example = "36")
    private long hoursBeforeDeparture;

    @Schema(description = "Human-readable message explaining the result")
    private String message;

    @Schema(description = "When the cancellation was created")
    private LocalDateTime createdAt;
}
