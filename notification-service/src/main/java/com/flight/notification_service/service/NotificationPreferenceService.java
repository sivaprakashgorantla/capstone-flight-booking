package com.flight.notification_service.service;

import com.flight.notification_service.dto.NotificationPreferenceRequest;
import com.flight.notification_service.dto.NotificationPreferenceResponse;
import com.flight.notification_service.exception.BadRequestException;
import com.flight.notification_service.exception.PreferenceNotFoundException;
import com.flight.notification_service.model.NotificationPreference;
import com.flight.notification_service.repository.NotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;

    // ── UC9 Step 1: Subscribe ─────────────────────────────────────────────────

    @Transactional
    public NotificationPreferenceResponse subscribe(NotificationPreferenceRequest request,
                                                     String userId,
                                                     String userEmail) {
        if (preferenceRepository.existsByUserId(userId)) {
            throw new BadRequestException(
                    "Preferences already exist for this user. Use PUT /notifications/preferences to update.");
        }

        NotificationPreference pref = NotificationPreference.builder()
                .userId(userId)
                .userEmail(userEmail)
                .emailEnabled(request.isEmailEnabled())
                .pushEnabled(request.isPushEnabled())
                .smsEnabled(request.isSmsEnabled())
                .bookingConfirmations(request.isBookingConfirmations())
                .delayAlerts(request.isDelayAlerts())
                .reminders(request.isReminders())
                .paymentAlerts(request.isPaymentAlerts())
                .cancellationAlerts(request.isCancellationAlerts())
                .generalAlerts(request.isGeneralAlerts())
                .build();

        NotificationPreference saved = preferenceRepository.save(pref);
        log.info("User {} subscribed to notifications", userId);
        return toResponse(saved, "Successfully subscribed to notifications");
    }

    // ── UC9 Step 3: Get preferences ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public NotificationPreferenceResponse getPreferences(String userId) {
        NotificationPreference pref = findByUserId(userId);
        return toResponse(pref, null);
    }

    // ── UC9 Step 3: Update preferences ───────────────────────────────────────

    @Transactional
    public NotificationPreferenceResponse updatePreferences(NotificationPreferenceRequest request,
                                                             String userId) {
        NotificationPreference pref = findByUserId(userId);

        pref.setEmailEnabled(request.isEmailEnabled());
        pref.setPushEnabled(request.isPushEnabled());
        pref.setSmsEnabled(request.isSmsEnabled());
        pref.setBookingConfirmations(request.isBookingConfirmations());
        pref.setDelayAlerts(request.isDelayAlerts());
        pref.setReminders(request.isReminders());
        pref.setPaymentAlerts(request.isPaymentAlerts());
        pref.setCancellationAlerts(request.isCancellationAlerts());
        pref.setGeneralAlerts(request.isGeneralAlerts());

        NotificationPreference saved = preferenceRepository.save(pref);
        log.info("User {} updated notification preferences", userId);
        return toResponse(saved, "Notification preferences updated successfully");
    }

    // ── Delete (opt-out) ──────────────────────────────────────────────────────

    @Transactional
    public void unsubscribe(String userId) {
        NotificationPreference pref = findByUserId(userId);
        preferenceRepository.delete(pref);
        log.info("User {} unsubscribed from all notifications", userId);
    }

    // ── Admin: all preferences ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<NotificationPreferenceResponse> getAllPreferences() {
        return preferenceRepository.findAll()
                .stream()
                .map(p -> toResponse(p, null))
                .toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Check if a notification TYPE is enabled for a user.
     * Falls back to true (send) when user has no stored preference.
     */
    public boolean isTypeEnabled(String userId, String notificationType) {
        return preferenceRepository.findByUserId(userId)
                .map(pref -> switch (notificationType) {
                    case "BOOKING_CONFIRMATION" -> pref.isBookingConfirmations();
                    case "DELAY_ALERT"          -> pref.isDelayAlerts();
                    case "FLIGHT_REMINDER"      -> pref.isReminders();
                    case "PAYMENT_SUCCESS"      -> pref.isPaymentAlerts();
                    case "CANCELLATION_CONFIRMATION" -> pref.isCancellationAlerts();
                    case "GENERAL_ALERT"        -> pref.isGeneralAlerts();
                    default                     -> true;
                })
                .orElse(true);   // no preference on file → send by default
    }

    /**
     * Returns the preferred CHANNEL for a user (EMAIL → PUSH → SMS fallback).
     */
    public com.flight.notification_service.model.NotificationChannel preferredChannel(String userId) {
        return preferenceRepository.findByUserId(userId)
                .map(pref -> {
                    if (pref.isEmailEnabled())  return com.flight.notification_service.model.NotificationChannel.EMAIL;
                    if (pref.isPushEnabled())   return com.flight.notification_service.model.NotificationChannel.PUSH;
                    return com.flight.notification_service.model.NotificationChannel.SMS;
                })
                .orElse(com.flight.notification_service.model.NotificationChannel.EMAIL);
    }

    private NotificationPreference findByUserId(String userId) {
        return preferenceRepository.findByUserId(userId)
                .orElseThrow(() -> new PreferenceNotFoundException(userId));
    }

    private NotificationPreferenceResponse toResponse(NotificationPreference p, String message) {
        return NotificationPreferenceResponse.builder()
                .id(p.getId())
                .userId(p.getUserId())
                .userEmail(p.getUserEmail())
                .emailEnabled(p.isEmailEnabled())
                .pushEnabled(p.isPushEnabled())
                .smsEnabled(p.isSmsEnabled())
                .bookingConfirmations(p.isBookingConfirmations())
                .delayAlerts(p.isDelayAlerts())
                .reminders(p.isReminders())
                .paymentAlerts(p.isPaymentAlerts())
                .cancellationAlerts(p.isCancellationAlerts())
                .generalAlerts(p.isGeneralAlerts())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .message(message)
                .build();
    }
}
