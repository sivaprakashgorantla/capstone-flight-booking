package com.flight.booking_service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmBookingRequest {
    private String bookingReference;
    private String paymentReference;
    private String status; // CONFIRMED or PAYMENT_FAILED
}
