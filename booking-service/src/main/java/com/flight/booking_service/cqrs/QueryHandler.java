package com.flight.booking_service.cqrs;

import java.lang.annotation.*;

/**
 * CQRS marker — Query Handler.
 *
 * Annotates service methods that only read state (read operations).
 * Query methods must never modify data and should use
 * {@code @Transactional(readOnly = true)} for optimised DB access.
 *
 * Examples: getBookingById, getMyBookings, getBookingByReference
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface QueryHandler {

    /** Short description of the query being handled. */
    String value() default "";
}
