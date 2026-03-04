package com.flight.payment_service.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmBookingRequest {
    private String bookingReference;
    private String paymentReference;
    private String status;   // "CONFIRMED" or "PAYMENT_FAILED"
}
