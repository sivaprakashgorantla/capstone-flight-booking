package com.flight.cancellation_service.service;

import com.flight.cancellation_service.model.Cancellation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

/**
 * Mock notification service — logs confirmation emails.
 * In production: replace with JavaMailSender / SendGrid / AWS SES.
 */
@Slf4j
@Service
public class NotificationService {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    // ── Cancellation Confirmation ─────────────────────────────────────────────

    public void sendCancellationConfirmation(Cancellation c) {
        String subject = "Booking Cancelled — " + c.getBookingReference()
                + " | Refund: Rs." + c.getRefundAmount();

        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║          CANCELLATION CONFIRMATION EMAIL SENT                ║");
        log.info("╠══════════════════════════════════════════════════════════════╣");
        log.info("║ To        : {}", c.getUserEmail());
        log.info("║ Subject   : {}", subject);
        log.info("╠══════════════════════════════════════════════════════════════╣");
        log.info("║");
        log.info("║  Dear {},", c.getUserId());
        log.info("║");
        log.info("║  Your booking has been CANCELLED successfully.");
        log.info("║");
        log.info("║  CANCELLATION DETAILS");
        log.info("║  Cancel Ref  : {}", c.getCancellationReference());
        log.info("║  Booking Ref : {}", c.getBookingReference());
        log.info("║  Flight      : {} ({})", c.getFlightNumber(), c.getAirline());
        log.info("║  Route       : {} → {}", c.getDepartureCity(), c.getDestinationCity());
        if (c.getDepartureTime() != null) {
            log.info("║  Departure   : {}", c.getDepartureTime().format(FMT));
        }
        log.info("║");
        log.info("║  REFUND SUMMARY");
        log.info("║  Original Amount : Rs.{}", c.getOriginalAmount());
        log.info("║  Refund % Applied: {}%", c.getRefundPercentage());
        log.info("║  Refund Amount   : Rs.{}", c.getRefundAmount());
        if (c.getRefundTransactionId() != null) {
            log.info("║  Refund Txn ID   : {}", c.getRefundTransactionId());
            log.info("║  Credit Timeline : 5–7 business days");
        } else {
            log.info("║  Refund          : Not applicable");
        }
        log.info("║");
        log.info("║  Reason: {}", c.getCancellationReason() != null
                ? c.getCancellationReason() : "Not specified");
        log.info("║");
        log.info("║  We hope to serve you again soon.");
        log.info("╚══════════════════════════════════════════════════════════════╝");
    }

    // ── No-Refund Notification ────────────────────────────────────────────────

    public void sendNoRefundNotification(Cancellation c, String reason) {
        log.warn("╔══════════════════════════════════════════════════════════════╗");
        log.warn("║          CANCELLATION — NO REFUND EMAIL SENT                 ║");
        log.warn("╠══════════════════════════════════════════════════════════════╣");
        log.warn("║ To       : {}", c.getUserEmail());
        log.warn("║ Booking  : {}", c.getBookingReference());
        log.warn("║ Cancel # : {}", c.getCancellationReference());
        log.warn("║ Reason   : {}", reason);
        log.warn("║ Policy   : Cancellations within 6 hours of departure or     ║");
        log.warn("║            unpaid bookings are not eligible for refund.      ║");
        log.warn("╚══════════════════════════════════════════════════════════════╝");
    }

    // ── Rejection Notification ────────────────────────────────────────────────

    public void sendRejectionNotification(String userEmail, String userId,
                                          String bookingRef, String reason) {
        log.warn("╔══════════════════════════════════════════════════════════════╗");
        log.warn("║          CANCELLATION REJECTED EMAIL SENT                    ║");
        log.warn("╠══════════════════════════════════════════════════════════════╣");
        log.warn("║ To       : {}", userEmail);
        log.warn("║ User     : {}", userId);
        log.warn("║ Booking  : {}", bookingRef);
        log.warn("║ Reason   : {}", reason);
        log.warn("╚══════════════════════════════════════════════════════════════╝");
    }
}
