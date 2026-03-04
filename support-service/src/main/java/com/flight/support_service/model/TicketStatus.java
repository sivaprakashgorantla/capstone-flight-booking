package com.flight.support_service.model;

public enum TicketStatus {
    OPEN,            // Submitted — awaiting agent assignment
    IN_PROGRESS,     // Assigned — agent working on it
    AWAITING_USER,   // Agent replied — waiting for user response
    RESOLVED,        // Issue resolved by agent
    CLOSED           // Closed after resolution or by user
}
