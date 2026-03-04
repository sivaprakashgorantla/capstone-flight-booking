package com.flight.notification_service.service;

import com.flight.notification_service.model.Notification;
import com.flight.notification_service.model.NotificationChannel;
import com.flight.notification_service.model.NotificationStatus;
import com.flight.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Simulates dispatching a notification over EMAIL / PUSH / SMS.
 * In production this would integrate with SendGrid, FCM, Twilio etc.
 * Here every delivery is logged to console and marked SENT.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDeliveryService {

    private final NotificationRepository notificationRepository;

    /**
     * Attempt delivery of the notification on its configured channel.
     * Updates status to SENT (or FAILED on error) and persists.
     */
    public Notification deliver(Notification notification) {
        try {
            if (notification.getChannel() == NotificationChannel.EMAIL) {
                sendEmail(notification);
            } else if (notification.getChannel() == NotificationChannel.PUSH) {
                sendPush(notification);
            } else {
                sendSms(notification);
            }

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            log.info("[DELIVERY] {} ref={} → SENT via {}",
                    notification.getType(),
                    notification.getNotificationReference(),
                    notification.getChannel());

        } catch (Exception e) {
            notification.setStatus(NotificationStatus.FAILED);
            log.error("[DELIVERY] {} ref={} → FAILED: {}",
                    notification.getType(),
                    notification.getNotificationReference(),
                    e.getMessage());
        }

        return notificationRepository.save(notification);
    }

    // ── Simulated channel dispatchers ─────────────────────────────────────────

    private void sendEmail(Notification n) {
        log.info("""
                ╔══════════════════════════════════════════════════════════════╗
                ║                    EMAIL NOTIFICATION                        ║
                ╠══════════════════════════════════════════════════════════════╣
                ║  To      : {}
                ║  Subject : {}
                ║  ──────────────────────────────────────────────────────────  ║
                ║  {}
                ╚══════════════════════════════════════════════════════════════╝
                """,
                n.getUserEmail(), n.getTitle(), n.getMessage());
    }

    private void sendPush(Notification n) {
        log.info("""
                ╔══════════════════════════════════════════╗
                ║           PUSH NOTIFICATION              ║
                ╠══════════════════════════════════════════╣
                ║  UserId : {}
                ║  Title  : {}
                ║  Body   : {}
                ╚══════════════════════════════════════════╝
                """,
                n.getUserId(), n.getTitle(), n.getMessage());
    }

    private void sendSms(Notification n) {
        log.info("""
                ╔═════════════════════════════════════════╗
                ║            SMS NOTIFICATION             ║
                ╠═════════════════════════════════════════╣
                ║  To  : {}
                ║  Msg : {}
                ╚═════════════════════════════════════════╝
                """,
                n.getUserEmail(), n.getTitle() + " — " + n.getMessage());
    }
}
