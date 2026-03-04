package com.flight.cancellation_service.repository;

import com.flight.cancellation_service.model.Cancellation;
import com.flight.cancellation_service.model.CancellationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CancellationRepository extends JpaRepository<Cancellation, Long> {

    Optional<Cancellation> findByCancellationReference(String cancellationReference);

    Optional<Cancellation> findByBookingId(Long bookingId);

    Optional<Cancellation> findByBookingReference(String bookingReference);

    List<Cancellation> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Cancellation> findAllByOrderByCreatedAtDesc();

    List<Cancellation> findByStatus(CancellationStatus status);

    boolean existsByCancellationReference(String cancellationReference);

    boolean existsByBookingId(Long bookingId);
}
