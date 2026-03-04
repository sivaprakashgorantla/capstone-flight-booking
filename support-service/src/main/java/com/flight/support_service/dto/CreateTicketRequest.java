package com.flight.support_service.dto;

import com.flight.support_service.model.TicketCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to submit a new customer support ticket")
public class CreateTicketRequest {

    @NotNull(message = "Category is required")
    @Schema(
        description = "Support category",
        example = "PAYMENT_ISSUE",
        allowableValues = {
            "BOOKING_ISSUE", "PAYMENT_ISSUE", "CANCELLATION_ISSUE",
            "REFUND_ISSUE", "FLIGHT_DELAY", "BAGGAGE",
            "GENERAL_ENQUIRY", "TECHNICAL_ISSUE"
        }
    )
    private TicketCategory category;

    @NotBlank(message = "Subject is required")
    @Size(min = 5, max = 200, message = "Subject must be between 5 and 200 characters")
    @Schema(description = "Brief subject of the issue", example = "Payment deducted but booking not confirmed")
    private String subject;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
    @Schema(
        description = "Detailed description of the issue",
        example = "I made a payment of Rs.9999 via UPI but my booking is still showing PENDING_PAYMENT."
    )
    private String description;

    @Schema(description = "Related booking reference, if any (optional)", example = "BKG-ABC12345")
    private String bookingReference;

    @Schema(description = "Related flight number, if any (optional)", example = "AI-202")
    private String flightNumber;
}
