package com.flight.cancellation_service.saga;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * UC6 — Cancellation & Refund Saga (Choreography-based)
 *
 * Documents and logs every step of the distributed cancellation transaction.
 * Mirrors the [BOOKING-SAGA] tag used in booking-service so that all saga
 * steps can be correlated across microservices from a single log stream.
 *
 * ┌──────────────────────────────────────────────────────────────────────────┐
 * │                    CANCELLATION SAGA FLOW                                │
 * │                                                                          │
 * │  [User] ──► [cancellation-service] initiateCancellation()               │
 * │               CANCEL-STEP-1: Validate eligibility                        │
 * │                    │                                                     │
 * │             ┌──────┴──────┐                                              │
 * │           REJECT        APPROVE                                          │
 * │             │               │                                            │
 * │    CANCEL-STEP-2         CANCEL-STEP-2                                   │
 * │    [REJECTED]            [booking-service] cancelBooking()               │
 * │    Notify user           CANCEL-STEP-3 [refund-gateway] processRefund()  │
 * │                               /                  \                       │
 * │                     SUCCESS  /                    \  GATEWAY ERROR       │
 * │                             /                      \                     │
 * │              CANCEL-STEP-4 [COMPLETED]    CANCEL-STEP-3 [REFUND_PENDING] │
 * │              status=REFUNDED              status=APPROVED (retry needed) │
 * │              Notify user                                                  │
 * └──────────────────────────────────────────────────────────────────────────┘
 *
 * COMPENSATING TRANSACTIONS:
 *  - Flight already departed  → REJECTED (no state change in booking)
 *  - Refund gateway failure   → booking CANCELLED, refund PENDING manual retry
 *  - Zero refund window       → booking CANCELLED, NO_REFUND status
 */
@Slf4j
@Component
public class CancellationSagaOrchestrator {

    private static final String SAGA_TAG = "[BOOKING-SAGA]";

    // ── CANCEL-STEP-1: Cancellation Initiated ────────────────────────────────

    /**
     * Called immediately after eligibility check passes and the cancellation
     * record is about to be created.
     */
    public void onCancellationInitiated(String bookingRef, String canRef, String bookingStatus) {
        log.info("{} CANCEL-STEP-1 [INITIATED] bookingRef={} canRef={} bookingStatus={}",
                SAGA_TAG, bookingRef, canRef, bookingStatus);
        log.info("{} CANCEL-STEP-1 Next: validate eligibility and determine refund policy",
                SAGA_TAG);
    }

    // ── CANCEL-STEP-2: Eligibility Check Rejected ────────────────────────────

    /**
     * Called when cancellation is rejected due to ineligibility
     * (flight departed, already cancelled, etc.). No state change occurs in booking-service.
     * This is a terminal state — no compensating action required.
     */
    public void onCancellationRejected(String bookingRef, String canRef, String reason) {
        log.warn("{} CANCEL-STEP-2 [REJECTED] bookingRef={} canRef={} reason={}",
                SAGA_TAG, bookingRef, canRef, reason);
        log.warn("{} CANCEL-STEP-2 Status: no booking state change — saga ends here", SAGA_TAG);
    }

    // ── CANCEL-STEP-3: Booking Cancelled, Refund Processing ─────────────────

    /**
     * Called after booking-service confirms the booking is CANCELLED
     * and the refund gateway call is about to begin.
     */
    public void onBookingCancelledRefundPending(String bookingRef, String canRef,
                                                 int refundPct, String refundAmount) {
        log.info("{} CANCEL-STEP-3 [BOOKING_CANCELLED] Refund pending — bookingRef={} canRef={} refund={}% amount={}",
                SAGA_TAG, bookingRef, canRef, refundPct, refundAmount);
        log.info("{} CANCEL-STEP-3 Status: CONFIRMED → CANCELLED  |  calling refund gateway",
                SAGA_TAG);
    }

    // ── CANCEL-STEP-3 (Degraded): Refund Gateway Failure ────────────────────

    /**
     * Called when cancellation succeeds but the refund gateway throws an error.
     * The booking is CANCELLED but refund must be retried manually.
     * This is a partial compensation — saga is in a degraded state.
     */
    public void onRefundGatewayFailure(String bookingRef, String canRef, String error) {
        log.error("{} CANCEL-STEP-3 [REFUND_PENDING] Gateway error — bookingRef={} canRef={} error={}",
                SAGA_TAG, bookingRef, canRef, error);
        log.error("{} CANCEL-STEP-3 Booking CANCELLED but refund PENDING manual retry — degraded state",
                SAGA_TAG);
    }

    // ── CANCEL-STEP-3 (No Refund): Zero-refund window ────────────────────────

    /**
     * Called when cancellation is within the zero-refund window (e.g. <6h before departure).
     * Booking is cancelled but no refund is issued. Not a failure — expected policy outcome.
     */
    public void onNoRefundCancellation(String bookingRef, String canRef, String policyDesc) {
        log.info("{} CANCEL-STEP-3 [NO_REFUND] bookingRef={} canRef={} policy={}",
                SAGA_TAG, bookingRef, canRef, policyDesc);
        log.info("{} CANCEL-STEP-3 Status: CONFIRMED → CANCELLED  |  no refund per policy", SAGA_TAG);
    }

    // ── CANCEL-STEP-4: Saga Completed ────────────────────────────────────────

    /**
     * Called when the full cancellation + refund saga completes successfully.
     */
    public void onCancellationCompleted(String bookingRef, String canRef,
                                         int refundPct, String refundAmount) {
        log.info("{} CANCEL-STEP-4 [COMPLETED] bookingRef={} canRef={} refund={}% amount={}",
                SAGA_TAG, bookingRef, canRef, refundPct, refundAmount);
        log.info("{} CANCEL-STEP-4 Status: CANCELLED + REFUNDED — saga completed successfully",
                SAGA_TAG);
    }

    // ── Free Cancel (PENDING_PAYMENT) ─────────────────────────────────────────

    /**
     * Called when a PENDING_PAYMENT booking is cancelled with no charge to the user.
     */
    public void onFreeCancellation(String bookingRef, String canRef) {
        log.info("{} CANCEL-FREE [FREE_CANCEL] No payment collected — bookingRef={} canRef={}",
                SAGA_TAG, bookingRef, canRef);
        log.info("{} CANCEL-FREE Status: PENDING_PAYMENT → CANCELLED  |  no refund needed",
                SAGA_TAG);
    }
}
