package com.flight.notification_service.model;

/**
 * Types of notifications the system can send.
 */
public enum NotificationType {

    /** UC9 Step 2a — sent after booking is confirmed by booking-service */
    BOOKING_CONFIRMATION,

    /** UC9 Step 2b — sent when a flight departure is delayed */
    DELAY_ALERT,

    /** UC9 Step 2c — pre-departure reminder (24h / 2h before flight) */
    FLIGHT_REMINDER,

    /** Sent after payment is processed successfully */
    PAYMENT_SUCCESS,

    /** Sent after a cancellation is processed */
    CANCELLATION_CONFIRMATION,

    /** General broadcast or admin-pushed message */
    GENERAL_ALERT
}
