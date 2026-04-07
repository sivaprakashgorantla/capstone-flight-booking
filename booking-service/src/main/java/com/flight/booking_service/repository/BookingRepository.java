package com.flight.booking_service.repository;

import com.flight.booking_service.model.Booking;
import com.flight.booking_service.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingReference(String bookingReference);
    List<Booking> findByUserIdOrderByCreatedAtDesc(String userId);
    List<Booking> findByFlightIdAndStatusIn(Long flightId, List<BookingStatus> statuses);
    List<Booking> findAllByOrderByCreatedAtDesc();
    boolean existsByBookingReference(String bookingReference);

    // Upcoming: CONFIRMED bookings where departure is in the future
    List<Booking> findByUserIdAndStatusAndDepartureTimeAfterOrderByDepartureTimeAsc(
            String userId, BookingStatus status, LocalDateTime now);

    // Completed: CONFIRMED bookings where arrival is in the past
    List<Booking> findByUserIdAndStatusAndArrivalTimeBeforeOrderByArrivalTimeDesc(
            String userId, BookingStatus status, LocalDateTime now);
}
