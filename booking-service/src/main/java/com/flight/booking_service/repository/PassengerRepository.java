package com.flight.booking_service.repository;

import com.flight.booking_service.model.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PassengerRepository extends JpaRepository<Passenger, Long> {

    /** All passengers belonging to a specific booking. */
    List<Passenger> findByBookingId(Long bookingId);

    /** Single passenger within a specific booking — enforces booking ownership. */
    Optional<Passenger> findByIdAndBookingId(Long id, Long bookingId);
}
