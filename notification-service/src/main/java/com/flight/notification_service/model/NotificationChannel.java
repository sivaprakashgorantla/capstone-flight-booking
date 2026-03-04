package com.flight.notification_service.model;

/**
 * Delivery channels available for notifications.
 * All channels are simulated (logged to console) — no real SMTP/FCM in this POC.
 */
public enum NotificationChannel {
    EMAIL,
    PUSH,
    SMS
}
