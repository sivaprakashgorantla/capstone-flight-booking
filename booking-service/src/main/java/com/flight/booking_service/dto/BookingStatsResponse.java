package com.flight.booking_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Booking statistics summary (Admin only)")
public class BookingStatsResponse {

    @Schema(description = "Total bookings in the system", example = "150")
    private long total;

    @Schema(description = "Bookings awaiting payment", example = "20")
    private long pendingPayment;

    @Schema(description = "Bookings in payment processing", example = "5")
    private long paymentProcessing;

    @Schema(description = "Confirmed bookings", example = "100")
    private long confirmed;

    @Schema(description = "Bookings where payment failed", example = "10")
    private long paymentFailed;

    @Schema(description = "Cancelled bookings", example = "15")
    private long cancelled;

    @Schema(description = "Total revenue from CONFIRMED bookings", example = "125000.00")
    private BigDecimal totalRevenue;
}
