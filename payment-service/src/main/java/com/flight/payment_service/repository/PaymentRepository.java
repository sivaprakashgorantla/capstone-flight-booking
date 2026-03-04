package com.flight.payment_service.repository;

import com.flight.payment_service.model.Payment;
import com.flight.payment_service.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentReference(String paymentReference);
    Optional<Payment> findByBookingId(Long bookingId);
    Optional<Payment> findByBookingReference(String bookingReference);
    List<Payment> findByUserIdOrderByCreatedAtDesc(String userId);
    List<Payment> findAllByOrderByCreatedAtDesc();
    boolean existsByPaymentReference(String paymentReference);
    List<Payment> findByStatus(PaymentStatus status);
}
