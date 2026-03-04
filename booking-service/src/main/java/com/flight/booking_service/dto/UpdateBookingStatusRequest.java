package com.flight.booking_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Admin request to manually override a booking's status")
public class UpdateBookingStatusRequest {

    @NotBlank(message = "Status is required")
    @Schema(
        description = "New booking status",
        example     = "CANCELLED",
        allowableValues = {
            "PENDING_PAYMENT",
            "PAYMENT_PROCESSING",
            "CONFIRMED",
            "PAYMENT_FAILED",
            "CANCELLED"
        }
    )
    private String status;

    @Schema(description = "Optional reason for the status change", example = "Manual override by admin")
    private String reason;
}
