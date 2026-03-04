package com.flight.reporting.exception;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) { super(message); }
}
