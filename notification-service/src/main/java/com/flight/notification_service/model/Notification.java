package com.flight.notification_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Persisted notification record.
 * Each notification sent (or attempted) creates one record.
 */
@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique reference e.g. NOTIF-A1B2C3D4 */
    @Column(name = "notification_reference", unique = true, nullable = false)
    private String notificationReference;

    /** Owner — from JWT userId claim */
    @Column(name = "user_id", nullable = false)
    private String userId;

    /** Owner email — from JWT username claim */
    @Column(name = "user_email")
    private String userEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    /** ID of the related entity (bookingId, flightId, paymentId …) */
    @Column(name = "related_entity_id")
    private String relatedEntityId;

    /** Type label of the related entity (BOOKING, FLIGHT, PAYMENT …) */
    @Column(name = "related_entity_type")
    private String relatedEntityType;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
