package com.flight.cancellation_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to cancel a confirmed booking and initiate a refund")
public class InitiateCancellationRequest {

    @NotNull(message = "Booking ID is required")
    @Schema(description = "Booking ID from POST /bookings", example = "1")
    private Long bookingId;

    @NotBlank(message = "Booking reference is required")
    @Schema(description = "Booking reference e.g. BKG-ABC12345", example = "BKG-ABC12345")
    private String bookingReference;

    @Schema(
        description = "Reason for cancellation (optional)",
        example = "Change of travel plans"
    )
    private String reason;
}
