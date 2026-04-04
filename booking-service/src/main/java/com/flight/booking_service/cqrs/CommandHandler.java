package com.flight.booking_service.cqrs;

import java.lang.annotation.*;

/**
 * CQRS marker — Command Handler.
 *
 * Annotates service methods that mutate state (write operations).
 * Commands should be transactional, idempotent where possible,
 * and must emit saga events on state transitions.
 *
 * Examples: createBooking, confirmBooking, cancelBooking, updatePassenger
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CommandHandler {

    /** Short description of the command being handled. */
    String value() default "";
}
