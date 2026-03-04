package com.flight.cancellation_service.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cancellations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cancellation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cancellation_reference", nullable = false, unique = true, length = 15)
    private String cancellationReference;      // CAN-XXXXXXXX

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "booking_reference", nullable = false, length = 15)
    private String bookingReference;           // BKG-XXXXXXXX

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "user_email", nullable = false, length = 150)
    private String userEmail;

    @Column(name = "flight_number", length = 20)
    private String flightNumber;

    @Column(length = 100)
    private String airline;

    @Column(name = "departure_city", length = 100)
    private String departureCity;

    @Column(name = "destination_city", length = 100)
    private String destinationCity;

    @Column(name = "departure_time")
    private LocalDateTime departureTime;

    /** Original total booking amount at time of cancellation. */
    @Column(name = "original_amount", precision = 10, scale = 2)
    private BigDecimal originalAmount;

    /** Refund percentage applied (0, 25, 50, 75, 90, 100). */
    @Column(name = "refund_percentage")
    private int refundPercentage;

    /** Calculated refund amount = originalAmount × refundPercentage / 100. */
    @Column(name = "refund_amount", precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CancellationStatus status = CancellationStatus.PENDING;

    /** Simulated refund gateway transaction ID. */
    @Column(name = "refund_transaction_id", length = 50)
    private String refundTransactionId;

    /** Hours before departure at the time of cancellation request (for audit). */
    @Column(name = "hours_before_departure")
    private long hoursBeforeDeparture;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
