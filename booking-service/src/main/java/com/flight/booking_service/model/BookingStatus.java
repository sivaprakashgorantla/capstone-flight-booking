package com.flight.booking_service.model;

public enum BookingStatus {
    PENDING_PAYMENT,
    PAYMENT_PROCESSING,
    CONFIRMED,
    PAYMENT_FAILED,
    CANCELLED
}
