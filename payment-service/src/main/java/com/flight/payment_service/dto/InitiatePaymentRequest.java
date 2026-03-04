package com.flight.payment_service.dto;

import com.flight.payment_service.model.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to initiate payment for a booking")
public class InitiatePaymentRequest {

    @NotNull(message = "Booking ID is required")
    @Schema(description = "Booking ID returned from POST /bookings", example = "1")
    private Long bookingId;

    @NotBlank(message = "Booking reference is required")
    @Schema(description = "Booking reference e.g. BKG-ABC12345", example = "BKG-ABC12345")
    private String bookingReference;

    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.01", message = "Total amount must be greater than 0")
    @Schema(description = "Total booking amount (from POST /bookings response)", example = "9999.00")
    private BigDecimal totalAmount;

    @NotNull(message = "Payment method is required")
    @Schema(description = "Payment method", example = "UPI",
            allowableValues = {"CREDIT_CARD", "DEBIT_CARD", "UPI", "NET_BANKING"})
    private PaymentMethod paymentMethod;

    @Schema(description = "Last 4 digits of card (for CREDIT_CARD / DEBIT_CARD)", example = "4242")
    private String cardLastFour;

    @Schema(description = "UPI ID (for UPI payments)", example = "user@upi")
    private String upiId;

    @Schema(description = "Set true to simulate payment failure (for testing)", example = "false")
    private boolean simulateFailure;
}
