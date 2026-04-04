package com.flight.booking_service.saga;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * UC3 / UC5 / UC6 — Booking Flow Saga (Choreography-based)
 *
 * This orchestrator documents and logs each step of the distributed
 * booking transaction. The saga uses the choreography pattern: each
 * microservice publishes the outcome of its local transaction via a
 * synchronous HTTP callback, and the next participant reacts accordingly.
 *
 * ┌──────────────────────────────────────────────────────────────────────┐
 * │                    BOOKING SAGA FLOW                                 │
 * │                                                                      │
 * │  [User] ──► [booking-service]  createBooking()                       │
 * │               Step 1: BOOKING CREATED → status = PENDING_PAYMENT     │
 * │                         │                                            │
 * │  [User] ──► [payment-service]  initiatePayment()                     │
 * │               Step 2: PAYMENT PROCESSING → gateway call              │
 * │                      /               \                               │
 * │            SUCCESS  /                 \  FAILURE                     │
 * │                    /                   \                             │
 * │  Step 3a [booking-service]         Step 3b [booking-service]         │
 * │  confirmBooking() → CONFIRMED      compensate() → PAYMENT_FAILED     │
 * │                                                                      │
 * │  ── CANCELLATION SAGA ──────────────────────────────────────────── │
 * │  [User] ──► [cancellation-service] initiateCancellation()            │
 * │               Step 1: Validate eligibility                           │
 * │               Step 2: [booking-service] cancel booking               │
 * │               Step 3: [refund-gateway] process refund                │
 * │               Step 4: Notify user (email)                            │
 * └──────────────────────────────────────────────────────────────────────┘
 *
 * COMPENSATING TRANSACTIONS:
 *  - Payment failed     → booking marked PAYMENT_FAILED (no seats released in POC)
 *  - Cancellation       → booking marked CANCELLED + refund initiated
 *  - Refund gateway err → cancellation stays APPROVED, manual retry needed
 */
@Slf4j
@Component
public class BookingSagaOrchestrator {

    private static final String SAGA_TAG = "[BOOKING-SAGA]";

    // ── Step 1 — Booking Created ─────────────────────────────────────────────

    /**
     * Called immediately after a booking is persisted with PENDING_PAYMENT.
     */
    public void onBookingCreated(String bookingRef, String userId,
                                  int passengerCount, String totalAmount) {
        log.info("{} STEP-1 [INITIATED] ref={} user={} passengers={} amount={}",
                SAGA_TAG, bookingRef, userId, passengerCount, totalAmount);
        log.info("{} STEP-1 Status: NEW → PENDING_PAYMENT", SAGA_TAG);
        log.info("{} STEP-1 Next: POST /payments/initiate  bookingRef={}", SAGA_TAG, bookingRef);
    }

    // ── Step 2 — Payment Processing ──────────────────────────────────────────

    /**
     * Called when payment-service begins processing.
     */
    public void onPaymentProcessing(String payRef, String bookingRef, String method) {
        log.info("{} STEP-2 [PAYMENT_PROCESSING] payRef={} bookingRef={} method={}",
                SAGA_TAG, payRef, bookingRef, method);
        log.info("{} STEP-2 Status: PENDING_PAYMENT → PAYMENT_PROCESSING", SAGA_TAG);
    }

    // ── Step 3a — Saga Completed ─────────────────────────────────────────────

    /**
     * Called when payment succeeds and booking is confirmed.
     * Saga completes successfully — no compensation needed.
     */
    public void onSagaCompleted(String bookingRef, String payRef) {
        log.info("{} STEP-3a [COMPLETED] Booking confirmed — bookingRef={} payRef={}",
                SAGA_TAG, bookingRef, payRef);
        log.info("{} STEP-3a Status: PAYMENT_PROCESSING → CONFIRMED", SAGA_TAG);
        log.info("{} STEP-3a Saga completed successfully for bookingRef={}", SAGA_TAG, bookingRef);
    }

    // ── Step 3b — Compensating Transaction ───────────────────────────────────

    /**
     * Called when payment fails. This is the compensating transaction:
     * the booking is rolled back to a terminal failure state so the
     * user can retry or the seat is freed for other bookings.
     */
    public void onSagaCompensated(String bookingRef, String payRef, String reason) {
        log.warn("{} STEP-3b [COMPENSATING] Payment failed — bookingRef={} payRef={} reason={}",
                SAGA_TAG, bookingRef, payRef, reason);
        log.warn("{} STEP-3b Status: PAYMENT_PROCESSING → PAYMENT_FAILED (compensated)",
                SAGA_TAG);
        log.warn("{} STEP-3b Compensating transaction applied for bookingRef={}", SAGA_TAG, bookingRef);
    }

    // ── Cancellation Saga ─────────────────────────────────────────────────────

    /**
     * Called when a cancellation saga begins.
     */
    public void onCancellationInitiated(String bookingRef, String canRef, String status) {
        log.info("{} CANCEL-STEP-1 [INITIATED] bookingRef={} canRef={} bookingStatus={}",
                SAGA_TAG, bookingRef, canRef, status);
    }

    /**
     * Called when cancellation + refund complete successfully.
     */
    public void onCancellationCompleted(String bookingRef, String canRef, int refundPct, String refundAmt) {
        log.info("{} CANCEL-STEP-4 [COMPLETED] bookingRef={} canRef={} refund={}% amount={}",
                SAGA_TAG, bookingRef, canRef, refundPct, refundAmt);
        log.info("{} CANCEL-STEP-4 Status: CONFIRMED → CANCELLED", SAGA_TAG);
    }

    /**
     * Called when cancellation is rejected (departed, already cancelled, etc.).
     */
    public void onCancellationRejected(String bookingRef, String canRef, String reason) {
        log.warn("{} CANCEL-STEP-2 [REJECTED] bookingRef={} canRef={} reason={}",
                SAGA_TAG, bookingRef, canRef, reason);
    }

    /**
     * Called when cancellation succeeds but refund gateway fails.
     * Booking is still cancelled but refund must be retried manually.
     * This is a partial compensation — saga is in a degraded state.
     */
    public void onRefundGatewayFailure(String bookingRef, String canRef, String error) {
        log.error("{} CANCEL-STEP-3 [REFUND_PENDING] Gateway error — bookingRef={} canRef={} error={}",
                SAGA_TAG, bookingRef, canRef, error);
        log.error("{} CANCEL-STEP-3 Booking CANCELLED but refund PENDING manual retry", SAGA_TAG);
    }
}
