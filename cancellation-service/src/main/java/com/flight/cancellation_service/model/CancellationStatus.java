package com.flight.cancellation_service.model;

public enum CancellationStatus {
    /** Cancellation request received and eligibility being verified. */
    PENDING,

    /** Eligible — refund processing initiated. */
    APPROVED,

    /** Refund successfully processed by the simulated gateway. */
    REFUNDED,

    /** Booking cancelled but no refund applies (< 6 h window or no payment collected). */
    NO_REFUND,

    /** Ineligible — flight already departed or booking already cancelled. */
    REJECTED
}
