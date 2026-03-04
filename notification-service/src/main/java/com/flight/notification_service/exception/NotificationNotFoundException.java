package com.flight.notification_service.exception;

public class NotificationNotFoundException extends RuntimeException {

    public NotificationNotFoundException(Long id) {
        super("Notification not found with ID: " + id);
    }

    public NotificationNotFoundException(String reference) {
        super("Notification not found with reference: " + reference);
    }
}
