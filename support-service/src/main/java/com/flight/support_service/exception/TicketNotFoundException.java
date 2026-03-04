package com.flight.support_service.exception;

public class TicketNotFoundException extends RuntimeException {

    public TicketNotFoundException(String message) {
        super(message);
    }

    public TicketNotFoundException(Long id) {
        super("Support ticket not found with ID: " + id);
    }
}
