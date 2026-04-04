package com.flight.cancellation_service.service;

import com.flight.cancellation_service.dto.*;
import com.flight.cancellation_service.exception.BadRequestException;
import com.flight.cancellation_service.exception.CancellationNotFoundException;
import com.flight.cancellation_service.model.Cancellation;
import com.flight.cancellation_service.model.CancellationStatus;
import com.flight.cancellation_service.repository.CancellationRepository;
import com.flight.cancellation_service.saga.CancellationSagaOrchestrator;
import org.springframework.security.access.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CancellationService {

    private final CancellationRepository cancellationRepository;
    private final BookingServiceClient bookingServiceClient;
    private final RefundService refundService;
    private final NotificationService notificationService;
    private final CancellationSagaOrchestrator sagaOrchestrator;

    // ══════════════════════════════════════════════════════════════════════════
    // UC6 Step 1-5: Initiate cancellation — full workflow
    // ══════════════════════════════════════════════════════════════════════════

    @Transactional
    public CancellationResponse initiateCancellation(InitiateCancellationRequest request,
                                                      String username) {
        log.info("Cancellation request — bookingId={}, bookingRef={}, user={}",
                request.getBookingId(), request.getBookingReference(), username);

        // ── Guard: already cancelled this booking? ────────────────────────────
        if (cancellationRepository.existsByBookingId(request.getBookingId())) {
            Cancellation existing = cancellationRepository
                    .findByBookingId(request.getBookingId()).orElseThrow();
            if (existing.getStatus() != CancellationStatus.REJECTED) {
                throw new BadRequestException(
                        "A cancellation already exists for booking "
                        + request.getBookingReference()
                        + " (cancel ref: " + existing.getCancellationReference() + ")");
            }
        }

        // ── Step 1: Fetch booking details from booking-service ────────────────
        BookingDetailsDTO booking = bookingServiceClient.getBookingDetails(request.getBookingId());

        // Ownership check
        if (!booking.getUserId().equals(username)) {
            throw new AccessDeniedException(
                    "Access denied — booking does not belong to user: " + username);
        }

        String bookingStatus = booking.getStatus();
        String canRef        = generateCancellationReference();

        // SAGA CANCEL-STEP-1 — initiation logged
        sagaOrchestrator.onCancellationInitiated(
                booking.getBookingReference(), canRef, bookingStatus);

        // ── Step 2: Verify eligibility ────────────────────────────────────────
        // Case A: Already cancelled / payment failed
        if ("CANCELLED".equals(bookingStatus) || "PAYMENT_FAILED".equals(bookingStatus)) {
            String rejectReason = "Booking is already in " + bookingStatus + " status.";
            Cancellation rejected = buildRejected(canRef, booking, request.getReason(), rejectReason);
            Cancellation saved = cancellationRepository.save(rejected);
            // SAGA CANCEL-STEP-2 REJECTED
            sagaOrchestrator.onCancellationRejected(
                    booking.getBookingReference(), canRef, rejectReason);
            notificationService.sendRejectionNotification(
                    booking.getUserEmail(), username,
                    booking.getBookingReference(), rejected.getCancellationReason());
            log.warn("Cancellation rejected — booking status is {}", bookingStatus);
            return toResponse(saved, "Cancellation rejected: booking is already " + bookingStatus);
        }

        // Case B: PENDING_PAYMENT — no money collected, free cancel
        if ("PENDING_PAYMENT".equals(bookingStatus)) {
            return processFreeCancel(canRef, booking, request.getReason(), username);
        }

        // Case C: CONFIRMED — check departure time and apply refund policy
        if ("CONFIRMED".equals(bookingStatus)) {
            return processConfirmedCancel(canRef, booking, request.getReason(), username);
        }

        throw new BadRequestException(
                "Cannot cancel booking with status: " + bookingStatus);
    }

    // ── Case B: PENDING_PAYMENT — free cancel, no refund needed ──────────────

    private CancellationResponse processFreeCancel(String canRef, BookingDetailsDTO booking,
                                                    String reason, String username) {
        log.info("Free cancel (PENDING_PAYMENT) — bookingId={}", booking.getId());

        Cancellation cancellation = Cancellation.builder()
                .cancellationReference(canRef)
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingReference())
                .userId(username)
                .userEmail(booking.getUserEmail())
                .flightNumber(booking.getFlightNumber())
                .airline(booking.getAirline())
                .departureCity(booking.getDepartureCity())
                .destinationCity(booking.getDestinationCity())
                .departureTime(booking.getDepartureTime())
                .originalAmount(booking.getTotalAmount())
                .refundPercentage(0)
                .refundAmount(BigDecimal.ZERO)
                .cancellationReason(reason != null ? reason : "User requested cancellation")
                .status(CancellationStatus.NO_REFUND)
                .hoursBeforeDeparture(booking.getDepartureTime() != null
                        ? refundService.hoursBeforeDeparture(booking.getDepartureTime()) : -1)
                .build();

        Cancellation saved = cancellationRepository.save(cancellation);

        // Step 3: Cancel booking in booking-service
        bookingServiceClient.cancelBooking(booking.getId());

        // SAGA CANCEL-FREE
        sagaOrchestrator.onFreeCancellation(booking.getBookingReference(), canRef);

        // Step 5: Confirmation email
        notificationService.sendNoRefundNotification(saved,
                "Booking was in PENDING_PAYMENT — no payment was collected.");

        log.info("Free cancellation complete: canRef={}", canRef);
        return toResponse(saved,
                "Booking cancelled successfully. No payment was collected, so no refund is required.");
    }

    // ── Case C: CONFIRMED — time-based refund policy ──────────────────────────

    private CancellationResponse processConfirmedCancel(String canRef, BookingDetailsDTO booking,
                                                         String reason, String username) {
        LocalDateTime departureTime = booking.getDepartureTime();

        // Step 2: Check if already departed
        if (departureTime != null && refundService.isFlightDeparted(departureTime)) {
            String rejectReason = "Flight has already departed — cancellation not allowed.";
            Cancellation rejected = buildRejected(canRef, booking, reason, rejectReason);
            Cancellation saved = cancellationRepository.save(rejected);
            // SAGA CANCEL-STEP-2 REJECTED
            sagaOrchestrator.onCancellationRejected(
                    booking.getBookingReference(), canRef, rejectReason);
            notificationService.sendRejectionNotification(
                    booking.getUserEmail(), username,
                    booking.getBookingReference(), rejected.getCancellationReason());
            log.warn("Cancellation rejected — flight already departed. bookingId={}",
                    booking.getId());
            return toResponse(saved, "Cancellation rejected: flight has already departed.");
        }

        // Calculate refund
        long hours = departureTime != null
                ? refundService.hoursBeforeDeparture(departureTime) : 999;
        int refundPct = refundService.calculateRefundPercentage(hours);
        BigDecimal refundAmt = refundService.calculateRefundAmount(
                booking.getTotalAmount(), refundPct);
        String policyDesc = refundService.policyDescription(hours);

        log.info("Refund policy: {}h before departure → {}% refund → Rs.{}",
                hours, refundPct, refundAmt);

        // Build cancellation record (APPROVED)
        Cancellation cancellation = Cancellation.builder()
                .cancellationReference(canRef)
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingReference())
                .userId(username)
                .userEmail(booking.getUserEmail())
                .flightNumber(booking.getFlightNumber())
                .airline(booking.getAirline())
                .departureCity(booking.getDepartureCity())
                .destinationCity(booking.getDestinationCity())
                .departureTime(departureTime)
                .originalAmount(booking.getTotalAmount())
                .refundPercentage(refundPct)
                .refundAmount(refundAmt)
                .cancellationReason(reason != null ? reason : "User requested cancellation")
                .status(CancellationStatus.APPROVED)
                .hoursBeforeDeparture(hours)
                .build();

        cancellation = cancellationRepository.save(cancellation);

        // Step 3: Cancel booking in booking-service
        bookingServiceClient.cancelBooking(booking.getId());

        // SAGA CANCEL-STEP-3 — booking cancelled, refund about to be processed
        sagaOrchestrator.onBookingCancelledRefundPending(
                booking.getBookingReference(), canRef, refundPct, refundAmt.toPlainString());

        // Step 4: Process refund
        String message;
        if (refundPct > 0) {
            try {
                String txnId = refundService.processRefund(refundAmt);
                cancellation.setRefundTransactionId(txnId);
                cancellation.setStatus(CancellationStatus.REFUNDED);
                cancellationRepository.save(cancellation);

                // SAGA CANCEL-STEP-4 COMPLETED
                sagaOrchestrator.onCancellationCompleted(
                        booking.getBookingReference(), canRef, refundPct, refundAmt.toPlainString());

                // Step 5: Confirmation email
                notificationService.sendCancellationConfirmation(cancellation);
                message = "Booking cancelled. Refund of Rs." + refundAmt
                        + " (" + refundPct + "%) initiated. Policy: " + policyDesc;
                log.info("Cancellation complete with refund: canRef={}, txn={}",
                        canRef, txnId);
            } catch (Exception e) {
                // Refund gateway failed — still cancelled, refund pending manual retry
                log.error("Refund gateway error: {}", e.getMessage());
                // SAGA CANCEL-STEP-3 DEGRADED — compensating state
                sagaOrchestrator.onRefundGatewayFailure(
                        booking.getBookingReference(), canRef, e.getMessage());
                cancellation.setStatus(CancellationStatus.APPROVED); // refund still pending
                cancellationRepository.save(cancellation);
                message = "Booking cancelled. Refund of Rs." + refundAmt
                        + " is pending — gateway error, will be retried automatically.";
            }
        } else {
            // 0% refund — within 6h window
            cancellation.setStatus(CancellationStatus.NO_REFUND);
            cancellationRepository.save(cancellation);

            // SAGA CANCEL-STEP-3 NO_REFUND
            sagaOrchestrator.onNoRefundCancellation(
                    booking.getBookingReference(), canRef, policyDesc);

            // Step 5: No-refund notification
            notificationService.sendNoRefundNotification(cancellation, policyDesc);
            message = "Booking cancelled. " + policyDesc + " — no refund applicable.";
            log.info("Cancellation with no refund: canRef={}", canRef);
        }

        return toResponse(cancellation, message);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Read operations
    // ══════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<CancellationResponse> getMyCancellations(String username) {
        log.info("Fetching cancellations for user={}", username);
        return cancellationRepository.findByUserIdOrderByCreatedAtDesc(username)
                .stream().map(c -> toResponse(c, null)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CancellationResponse getById(Long id, String username) {
        Cancellation c = cancellationRepository.findById(id)
                .orElseThrow(() -> new CancellationNotFoundException(
                        "Cancellation not found with id: " + id));
        if (!c.getUserId().equals(username)) {
            throw new AccessDeniedException("Access denied to cancellation id: " + id);
        }
        return toResponse(c, null);
    }

    @Transactional(readOnly = true)
    public CancellationResponse getByReference(String ref, String username) {
        Cancellation c = cancellationRepository.findByCancellationReference(ref)
                .orElseThrow(() -> new CancellationNotFoundException(
                        "Cancellation not found: " + ref));
        if (!c.getUserId().equals(username)) {
            throw new AccessDeniedException("Access denied to cancellation: " + ref);
        }
        return toResponse(c, null);
    }

    @Transactional(readOnly = true)
    public CancellationResponse getByBookingId(Long bookingId, String username) {
        Cancellation c = cancellationRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new CancellationNotFoundException(
                        "No cancellation found for booking: " + bookingId));
        if (!c.getUserId().equals(username)) {
            throw new AccessDeniedException("Access denied to cancellation for booking: "
                    + bookingId);
        }
        return toResponse(c, null);
    }

    @Transactional(readOnly = true)
    public List<CancellationResponse> getAllCancellations() {
        return cancellationRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(c -> toResponse(c, null)).collect(Collectors.toList());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Private helpers
    // ══════════════════════════════════════════════════════════════════════════

    private Cancellation buildRejected(String canRef, BookingDetailsDTO booking,
                                        String reason, String autoReason) {
        return Cancellation.builder()
                .cancellationReference(canRef)
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingReference())
                .userId(booking.getUserId())
                .userEmail(booking.getUserEmail())
                .flightNumber(booking.getFlightNumber())
                .airline(booking.getAirline())
                .departureCity(booking.getDepartureCity())
                .destinationCity(booking.getDestinationCity())
                .departureTime(booking.getDepartureTime())
                .originalAmount(booking.getTotalAmount())
                .refundPercentage(0)
                .refundAmount(BigDecimal.ZERO)
                .cancellationReason(reason != null ? reason + " | " + autoReason : autoReason)
                .status(CancellationStatus.REJECTED)
                .hoursBeforeDeparture(booking.getDepartureTime() != null
                        ? refundService.hoursBeforeDeparture(booking.getDepartureTime()) : 0)
                .build();
    }

    private String generateCancellationReference() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder("CAN-");
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        String ref = sb.toString();
        return cancellationRepository.existsByCancellationReference(ref)
                ? generateCancellationReference() : ref;
    }

    private CancellationResponse toResponse(Cancellation c, String message) {
        return CancellationResponse.builder()
                .id(c.getId())
                .cancellationReference(c.getCancellationReference())
                .bookingId(c.getBookingId())
                .bookingReference(c.getBookingReference())
                .userId(c.getUserId())
                .userEmail(c.getUserEmail())
                .flightNumber(c.getFlightNumber())
                .airline(c.getAirline())
                .departureCity(c.getDepartureCity())
                .destinationCity(c.getDestinationCity())
                .departureTime(c.getDepartureTime())
                .originalAmount(c.getOriginalAmount())
                .refundPercentage(c.getRefundPercentage())
                .refundAmount(c.getRefundAmount())
                .cancellationReason(c.getCancellationReason())
                .status(c.getStatus().name())
                .refundTransactionId(c.getRefundTransactionId())
                .hoursBeforeDeparture(c.getHoursBeforeDeparture())
                .message(message)
                .createdAt(c.getCreatedAt())
                .build();
    }
}
