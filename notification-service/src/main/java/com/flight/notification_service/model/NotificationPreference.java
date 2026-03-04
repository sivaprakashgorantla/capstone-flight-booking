package com.flight.notification_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Stores per-user notification preferences (UC9 Step 1 & 3).
 * One row per user, keyed by userId from the JWT.
 */
@Entity
@Table(name = "notification_preferences")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** JWT subject / userId — unique per user */
    @Column(name = "user_id", unique = true, nullable = false)
    private String userId;

    /** Email from JWT username claim */
    @Column(name = "user_email")
    private String userEmail;

    // ── Channel toggles ───────────────────────────────────────────────────────

    @Builder.Default
    @Column(name = "email_enabled")
    private boolean emailEnabled = true;

    @Builder.Default
    @Column(name = "push_enabled")
    private boolean pushEnabled = true;

    @Builder.Default
    @Column(name = "sms_enabled")
    private boolean smsEnabled = false;

    // ── Notification type toggles ─────────────────────────────────────────────

    /** UC9 Step 2a — booking confirmations */
    @Builder.Default
    @Column(name = "booking_confirmations")
    private boolean bookingConfirmations = true;

    /** UC9 Step 2b — flight delay alerts */
    @Builder.Default
    @Column(name = "delay_alerts")
    private boolean delayAlerts = true;

    /** UC9 Step 2c — pre-departure reminders */
    @Builder.Default
    @Column(name = "reminders")
    private boolean reminders = true;

    /** Payment success / failure alerts */
    @Builder.Default
    @Column(name = "payment_alerts")
    private boolean paymentAlerts = true;

    /** Cancellation confirmations */
    @Builder.Default
    @Column(name = "cancellation_alerts")
    private boolean cancellationAlerts = true;

    /** General system broadcasts */
    @Builder.Default
    @Column(name = "general_alerts")
    private boolean generalAlerts = true;

    // ── Audit ─────────────────────────────────────────────────────────────────

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
