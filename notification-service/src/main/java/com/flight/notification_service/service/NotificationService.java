package com.flight.notification_service.service;

import com.flight.notification_service.dto.*;
import com.flight.notification_service.exception.BadRequestException;
import com.flight.notification_service.exception.NotificationNotFoundException;
import com.flight.notification_service.model.*;
import com.flight.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository              notificationRepository;
    private final NotificationPreferenceService       preferenceService;
    private final NotificationDeliveryService         deliveryService;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");

    // ═════════════════════════════════════════════════════════════════════════
    // UC9 Step 2a — Booking Confirmation / Payment Success
    // ═════════════════════════════════════════════════════════════════════════

    @Transactional
    public NotificationResponse sendBookingNotification(BookingNotificationRequest req) {

        NotificationType type = parseType(req.getType());
        log.info("[UC9-2a] {} for userId={} booking={}", type, req.getUserId(), req.getBookingReference());

        // Respect user preference
        if (!preferenceService.isTypeEnabled(req.getUserId(), type.name())) {
            log.info("User {} has {} disabled — skipping", req.getUserId(), type);
            return buildSkippedResponse(req.getUserId(), type);
        }

        String title;
        String message;

        if (type == NotificationType.BOOKING_CONFIRMATION) {
            title = "Booking Confirmed — " + req.getBookingReference();
            message = String.format(
                    "Your booking %s has been confirmed!\n" +
                    "Flight: %s | %s → %s\n" +
                    "Departure: %s\n" +
                    "Passengers: %d | Total Paid: ₹%s\n" +
                    "Have a great flight!",
                    req.getBookingReference(),
                    req.getFlightNumber(), req.getDepartureCity(), req.getDestinationCity(),
                    req.getDepartureTime() != null ? req.getDepartureTime().format(FMT) : "—",
                    req.getPassengerCount(),
                    req.getTotalAmount() != null ? req.getTotalAmount().toPlainString() : "—"
            );
        } else {  // PAYMENT_SUCCESS
            title = "Payment Successful — " + req.getBookingReference();
            message = String.format(
                    "Payment of ₹%s received for booking %s.\n" +
                    "Flight: %s | %s → %s\n" +
                    "Your seat is confirmed!",
                    req.getTotalAmount() != null ? req.getTotalAmount().toPlainString() : "—",
                    req.getBookingReference(),
                    req.getFlightNumber(), req.getDepartureCity(), req.getDestinationCity()
            );
        }

        Notification notification = buildAndSave(
                req.getUserId(), req.getUserEmail(), type, title, message,
                req.getBookingReference(), "BOOKING"
        );
        return toResponse(notification, "Notification sent successfully");
    }

    // ═════════════════════════════════════════════════════════════════════════
    // UC9 Step 2b — Delay Alert
    // ═════════════════════════════════════════════════════════════════════════

    @Transactional
    public NotificationResponse sendDelayAlert(DelayAlertRequest req) {

        log.info("[UC9-2b] DELAY_ALERT userId={} flight={} delay={}min",
                req.getUserId(), req.getFlightNumber(), req.getDelayMinutes());

        if (!preferenceService.isTypeEnabled(req.getUserId(), NotificationType.DELAY_ALERT.name())) {
            log.info("User {} has DELAY_ALERT disabled — skipping", req.getUserId());
            return buildSkippedResponse(req.getUserId(), NotificationType.DELAY_ALERT);
        }

        String title = "⚠ Flight Delay — " + req.getFlightNumber();
        String message = String.format(
                "Your flight %s has been delayed by %d minutes.\n" +
                "Original Departure : %s\n" +
                "Revised Departure  : %s\n" +
                "Reason: %s\n" +
                "We apologise for the inconvenience.",
                req.getFlightNumber(),
                req.getDelayMinutes(),
                req.getOriginalDeparture() != null ? req.getOriginalDeparture().format(FMT) : "—",
                req.getNewDeparture() != null ? req.getNewDeparture().format(FMT) : "—",
                req.getReason() != null ? req.getReason() : "Operational reasons"
        );

        Notification notification = buildAndSave(
                req.getUserId(), req.getUserEmail(),
                NotificationType.DELAY_ALERT, title, message,
                req.getBookingReference(), "FLIGHT"
        );
        return toResponse(notification, "Delay alert sent");
    }

    // ═════════════════════════════════════════════════════════════════════════
    // UC9 Step 2c — Flight Reminder
    // ═════════════════════════════════════════════════════════════════════════

    @Transactional
    public NotificationResponse sendReminder(ReminderRequest req) {

        log.info("[UC9-2c] FLIGHT_REMINDER userId={} flight={} {}h before",
                req.getUserId(), req.getFlightNumber(), req.getHoursBeforeDeparture());

        if (!preferenceService.isTypeEnabled(req.getUserId(), NotificationType.FLIGHT_REMINDER.name())) {
            log.info("User {} has FLIGHT_REMINDER disabled — skipping", req.getUserId());
            return buildSkippedResponse(req.getUserId(), NotificationType.FLIGHT_REMINDER);
        }

        String title = String.format("Reminder: %dh until Flight %s",
                req.getHoursBeforeDeparture(), req.getFlightNumber());
        String message = String.format(
                "Your flight %s departs in %d hour(s)!\n" +
                "Route: %s → %s\n" +
                "Departure: %s\n" +
                "Booking Ref: %s\n" +
                "Please reach the airport at least 2 hours before departure.",
                req.getFlightNumber(),
                req.getHoursBeforeDeparture(),
                req.getDepartureCity(), req.getDestinationCity(),
                req.getDepartureTime() != null ? req.getDepartureTime().format(FMT) : "—",
                req.getBookingReference()
        );

        Notification notification = buildAndSave(
                req.getUserId(), req.getUserEmail(),
                NotificationType.FLIGHT_REMINDER, title, message,
                req.getBookingReference(), "BOOKING"
        );
        return toResponse(notification, "Reminder sent");
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Cancellation Confirmation
    // ═════════════════════════════════════════════════════════════════════════

    @Transactional
    public NotificationResponse sendCancellationNotification(CancellationNotificationRequest req) {

        log.info("[UC9] CANCELLATION_CONFIRMATION userId={} booking={}",
                req.getUserId(), req.getBookingReference());

        if (!preferenceService.isTypeEnabled(req.getUserId(),
                NotificationType.CANCELLATION_CONFIRMATION.name())) {
            return buildSkippedResponse(req.getUserId(), NotificationType.CANCELLATION_CONFIRMATION);
        }

        String title = "Booking Cancelled — " + req.getBookingReference();
        String message = String.format(
                "Your booking %s has been successfully cancelled.\n" +
                "Cancellation Ref: %s\n" +
                "Original Amount : ₹%s\n" +
                "Refund Amount   : ₹%s (%d%%)\n" +
                "%s",
                req.getBookingReference(),
                req.getCancellationReference() != null ? req.getCancellationReference() : "—",
                req.getOriginalAmount() != null ? req.getOriginalAmount().toPlainString() : "—",
                req.getRefundAmount() != null ? req.getRefundAmount().toPlainString() : "0",
                req.getRefundPercentage(),
                req.getRefundTransactionId() != null
                        ? "Refund TxnId: " + req.getRefundTransactionId() + " (5–7 business days)"
                        : "No refund applicable."
        );

        Notification notification = buildAndSave(
                req.getUserId(), req.getUserEmail(),
                NotificationType.CANCELLATION_CONFIRMATION, title, message,
                req.getBookingReference(), "CANCELLATION"
        );
        return toResponse(notification, "Cancellation notification sent");
    }

    // ═════════════════════════════════════════════════════════════════════════
    // General Alert (Admin broadcast)
    // ═════════════════════════════════════════════════════════════════════════

    @Transactional
    public NotificationResponse sendGeneralAlert(GeneralAlertRequest req) {

        log.info("[UC9] GENERAL_ALERT userId={} title={}", req.getUserId(), req.getTitle());

        if (!preferenceService.isTypeEnabled(req.getUserId(),
                NotificationType.GENERAL_ALERT.name())) {
            return buildSkippedResponse(req.getUserId(), NotificationType.GENERAL_ALERT);
        }

        Notification notification = buildAndSave(
                req.getUserId(), req.getUserEmail(),
                NotificationType.GENERAL_ALERT, req.getTitle(), req.getMessage(),
                null, "SYSTEM"
        );
        return toResponse(notification, "General alert sent");
    }

    // ═════════════════════════════════════════════════════════════════════════
    // User read operations
    // ═════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(n -> toResponse(n, null)).toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyUnread(String userId) {
        return notificationRepository
                .findByUserIdAndStatusNotOrderByCreatedAtDesc(userId, NotificationStatus.READ)
                .stream().map(n -> toResponse(n, null)).toList();
    }

    @Transactional(readOnly = true)
    public long countUnread(String userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    @Transactional
    public NotificationResponse markAsRead(Long id, String userId) {
        Notification n = findById(id);
        if (!n.getUserId().equals(userId)) throw new NotificationNotFoundException(id);
        if (n.getStatus() == NotificationStatus.READ) {
            return toResponse(n, "Already marked as read");
        }
        n.setStatus(NotificationStatus.READ);
        n.setReadAt(LocalDateTime.now());
        return toResponse(notificationRepository.save(n), "Marked as read");
    }

    @Transactional
    public int markAllAsRead(String userId) {
        List<Notification> unread = notificationRepository
                .findByUserIdAndStatusNotOrderByCreatedAtDesc(userId, NotificationStatus.READ);
        LocalDateTime now = LocalDateTime.now();
        unread.forEach(n -> { n.setStatus(NotificationStatus.READ); n.setReadAt(now); });
        notificationRepository.saveAll(unread);
        log.info("Marked {} notifications as read for userId={}", unread.size(), userId);
        return unread.size();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Admin operations
    // ═════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<NotificationResponse> getAllNotifications() {
        return notificationRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(n -> toResponse(n, null)).toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getByStatus(String statusStr) {
        NotificationStatus status = parseStatus(statusStr);
        return notificationRepository.findByStatus(status)
                .stream().map(n -> toResponse(n, null)).toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getByType(String typeStr) {
        NotificationType type = parseType(typeStr);
        return notificationRepository.findByType(type)
                .stream().map(n -> toResponse(n, null)).toList();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Helpers
    // ═════════════════════════════════════════════════════════════════════════

    private Notification buildAndSave(String userId, String userEmail,
                                      NotificationType type, String title, String message,
                                      String relatedId, String relatedType) {
        NotificationChannel channel = preferenceService.preferredChannel(userId);

        Notification n = Notification.builder()
                .notificationReference(generateReference())
                .userId(userId)
                .userEmail(userEmail)
                .type(type)
                .channel(channel)
                .status(NotificationStatus.PENDING)
                .title(title)
                .message(message)
                .relatedEntityId(relatedId)
                .relatedEntityType(relatedType)
                .build();

        Notification saved = notificationRepository.save(n);
        return deliveryService.deliver(saved);  // sends + updates status
    }

    private NotificationResponse buildSkippedResponse(String userId, NotificationType type) {
        return NotificationResponse.builder()
                .userId(userId)
                .type(type.name())
                .status(NotificationStatus.FAILED.name())
                .responseMessage("Notification skipped — user has disabled " + type.name())
                .build();
    }

    private Notification findById(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException(id));
    }

    private String generateReference() {
        String ref = "NOTIF-" + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 8).toUpperCase();
        return notificationRepository.existsByNotificationReference(ref)
                ? generateReference() : ref;
    }

    private NotificationType parseType(String value) {
        try {
            return NotificationType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid notification type: " + value);
        }
    }

    private NotificationStatus parseStatus(String value) {
        try {
            return NotificationStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + value +
                    ". Allowed: PENDING, SENT, FAILED, READ");
        }
    }

    private NotificationResponse toResponse(Notification n, String responseMessage) {
        return NotificationResponse.builder()
                .id(n.getId())
                .notificationReference(n.getNotificationReference())
                .userId(n.getUserId())
                .userEmail(n.getUserEmail())
                .type(n.getType() != null ? n.getType().name() : null)
                .channel(n.getChannel() != null ? n.getChannel().name() : null)
                .status(n.getStatus() != null ? n.getStatus().name() : null)
                .title(n.getTitle())
                .message(n.getMessage())
                .relatedEntityId(n.getRelatedEntityId())
                .relatedEntityType(n.getRelatedEntityType())
                .sentAt(n.getSentAt())
                .readAt(n.getReadAt())
                .createdAt(n.getCreatedAt())
                .responseMessage(responseMessage)
                .build();
    }
}
