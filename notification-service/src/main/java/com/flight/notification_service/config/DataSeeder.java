package com.flight.notification_service.config;

import com.flight.notification_service.model.*;
import com.flight.notification_service.repository.NotificationPreferenceRepository;
import com.flight.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Seeds sample preferences and notifications on startup (idempotent).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationRepository           notificationRepository;

    private static final List<String> SEED_REFS = List.of(
            "NOTIF-SEED0001", "NOTIF-SEED0002", "NOTIF-SEED0003",
            "NOTIF-SEED0004", "NOTIF-SEED0005"
    );

    @Override
    public void run(String... args) {
        seedPreferences();
        seedNotifications();
    }

    // ── Preferences ───────────────────────────────────────────────────────────

    private void seedPreferences() {
        String[] seedUsers = { "user-001", "user-002", "user-003" };
        String[] seedEmails = {
            "alice@example.com", "bob@example.com", "carol@example.com"
        };

        for (int i = 0; i < seedUsers.length; i++) {
            if (!preferenceRepository.existsByUserId(seedUsers[i])) {
                boolean allEnabled = i != 2;  // carol has some disabled
                preferenceRepository.save(NotificationPreference.builder()
                        .userId(seedUsers[i])
                        .userEmail(seedEmails[i])
                        .emailEnabled(true)
                        .pushEnabled(allEnabled)
                        .smsEnabled(false)
                        .bookingConfirmations(true)
                        .delayAlerts(allEnabled)
                        .reminders(allEnabled)
                        .paymentAlerts(true)
                        .cancellationAlerts(true)
                        .generalAlerts(allEnabled)
                        .build());
                log.info("[DataSeeder] Seeded preferences for {}", seedUsers[i]);
            }
        }
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    private void seedNotifications() {
        long existing = SEED_REFS.stream()
                .filter(notificationRepository::existsByNotificationReference)
                .count();
        if (existing == SEED_REFS.size()) {
            log.info("[DataSeeder] Notification seed data already present — skipping.");
            return;
        }

        // 1 — Booking Confirmation (SENT + READ)
        saveIfAbsent(Notification.builder()
                .notificationReference("NOTIF-SEED0001")
                .userId("user-001")
                .userEmail("alice@example.com")
                .type(NotificationType.BOOKING_CONFIRMATION)
                .channel(NotificationChannel.EMAIL)
                .status(NotificationStatus.READ)
                .title("Booking Confirmed — BKG-A1B2C3D4")
                .message("Your booking BKG-A1B2C3D4 has been confirmed! Flight AI-202 | Delhi → Mumbai | 15-Mar-2026 10:30")
                .relatedEntityId("BKG-A1B2C3D4")
                .relatedEntityType("BOOKING")
                .sentAt(LocalDateTime.now().minusDays(3))
                .readAt(LocalDateTime.now().minusDays(3).plusHours(1))
                .build());

        // 2 — Payment Success (SENT)
        saveIfAbsent(Notification.builder()
                .notificationReference("NOTIF-SEED0002")
                .userId("user-001")
                .userEmail("alice@example.com")
                .type(NotificationType.PAYMENT_SUCCESS)
                .channel(NotificationChannel.EMAIL)
                .status(NotificationStatus.SENT)
                .title("Payment Successful — BKG-A1B2C3D4")
                .message("Payment of ₹12,500 received for booking BKG-A1B2C3D4. Your seat is confirmed!")
                .relatedEntityId("BKG-A1B2C3D4")
                .relatedEntityType("BOOKING")
                .sentAt(LocalDateTime.now().minusDays(3))
                .build());

        // 3 — Delay Alert (SENT)
        saveIfAbsent(Notification.builder()
                .notificationReference("NOTIF-SEED0003")
                .userId("user-002")
                .userEmail("bob@example.com")
                .type(NotificationType.DELAY_ALERT)
                .channel(NotificationChannel.EMAIL)
                .status(NotificationStatus.SENT)
                .title("⚠ Flight Delay — 6E-301")
                .message("Your flight 6E-301 has been delayed by 120 minutes. New departure: 01-Mar-2026 14:00. Reason: Air traffic congestion.")
                .relatedEntityId("BKG-XY120000")
                .relatedEntityType("FLIGHT")
                .sentAt(LocalDateTime.now().minusDays(1))
                .build());

        // 4 — Flight Reminder (SENT)
        saveIfAbsent(Notification.builder()
                .notificationReference("NOTIF-SEED0004")
                .userId("user-002")
                .userEmail("bob@example.com")
                .type(NotificationType.FLIGHT_REMINDER)
                .channel(NotificationChannel.PUSH)
                .status(NotificationStatus.SENT)
                .title("Reminder: 24h until Flight 6E-301")
                .message("Your flight 6E-301 departs in 24 hours! Route: Bangalore → Hyderabad. Please reach the airport at least 2 hours before departure.")
                .relatedEntityId("BKG-XY120000")
                .relatedEntityType("BOOKING")
                .sentAt(LocalDateTime.now().minusHours(24))
                .build());

        // 5 — General Alert (PENDING / unread)
        saveIfAbsent(Notification.builder()
                .notificationReference("NOTIF-SEED0005")
                .userId("user-003")
                .userEmail("carol@example.com")
                .type(NotificationType.GENERAL_ALERT)
                .channel(NotificationChannel.EMAIL)
                .status(NotificationStatus.SENT)
                .title("Scheduled Maintenance on 10-Mar")
                .message("Our platform will be down for maintenance from 02:00–04:00 IST on 10-Mar-2026. We apologise for the inconvenience.")
                .relatedEntityId(null)
                .relatedEntityType("SYSTEM")
                .sentAt(LocalDateTime.now().minusHours(6))
                .build());

        log.info("[DataSeeder] ✓ Sample notifications seeded successfully.");
    }

    private void saveIfAbsent(Notification n) {
        if (!notificationRepository.existsByNotificationReference(n.getNotificationReference())) {
            notificationRepository.save(n);
        }
    }
}
