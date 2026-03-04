package com.flight.payment_service.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_reference", nullable = false, unique = true, length = 20)
    private String paymentReference;   // PAY-XXXXXXXX

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "booking_reference", nullable = false, length = 15)
    private String bookingReference;   // BKG-XXXXXXXX

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "transaction_id", length = 50)
    private String transactionId;      // simulated gateway TXN ID

    @Column(name = "gateway_response", length = 500)
    private String gatewayResponse;

    @Column(name = "failure_reason", length = 300)
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
