package com.flight.notification_service.model;

public enum NotificationStatus {
    PENDING,    // Created but not yet sent
    SENT,       // Successfully delivered
    FAILED,     // Delivery attempted but failed
    READ        // User has read/acknowledged the notification
}
