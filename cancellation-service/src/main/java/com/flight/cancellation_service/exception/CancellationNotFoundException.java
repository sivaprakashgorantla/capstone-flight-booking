package com.flight.cancellation_service.exception;

public class CancellationNotFoundException extends RuntimeException {
    public CancellationNotFoundException(String message) {
        super(message);
    }
}
