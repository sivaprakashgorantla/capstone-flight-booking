package com.flight.notification_service.exception;

public class PreferenceNotFoundException extends RuntimeException {

    public PreferenceNotFoundException(String userId) {
        super("No notification preferences found for user: " + userId +
                ". Please subscribe first via POST /notifications/preferences");
    }
}
