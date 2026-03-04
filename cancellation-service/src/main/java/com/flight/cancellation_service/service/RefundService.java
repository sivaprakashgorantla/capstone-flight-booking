package com.flight.cancellation_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;

/**
 * Encapsulates refund eligibility rules and simulates the refund gateway.
 *
 * Refund Policy (time before scheduled departure):
 *   > 48 hours   → 90 % refund
 *   24 – 48 hours → 75 % refund
 *   12 – 24 hours → 50 % refund
 *    6 – 12 hours → 25 % refund
 *    < 6 hours    →  0 % refund (NO_REFUND, but booking still cancelled)
 *   Already departed → Ineligible (REJECTED)
 */
@Slf4j
@Service
public class RefundService {

    // ── Eligibility ───────────────────────────────────────────────────────────

    /**
     * @return hours remaining before departure (negative = already departed)
     */
    public long hoursBeforeDeparture(LocalDateTime departureTime) {
        return ChronoUnit.HOURS.between(LocalDateTime.now(), departureTime);
    }

    /**
     * @return true if the flight has already departed.
     */
    public boolean isFlightDeparted(LocalDateTime departureTime) {
        return LocalDateTime.now().isAfter(departureTime);
    }

    /**
     * Calculate the refund percentage based on hours before departure.
     *
     * @param hours hours remaining before departure
     * @return refund percentage (0, 25, 50, 75, or 90)
     */
    public int calculateRefundPercentage(long hours) {
        if (hours > 48)       return 90;
        else if (hours > 24)  return 75;
        else if (hours > 12)  return 50;
        else if (hours > 6)   return 25;
        else                  return 0;
    }

    /**
     * Calculate the actual refund amount.
     */
    public BigDecimal calculateRefundAmount(BigDecimal originalAmount, int refundPercentage) {
        if (refundPercentage == 0) return BigDecimal.ZERO;
        return originalAmount
                .multiply(BigDecimal.valueOf(refundPercentage))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /**
     * Human-readable policy description for the given hours before departure.
     */
    public String policyDescription(long hours) {
        if (hours > 48)      return "More than 48 hours before departure — 90% refund";
        else if (hours > 24) return "24–48 hours before departure — 75% refund";
        else if (hours > 12) return "12–24 hours before departure — 50% refund";
        else if (hours > 6)  return "6–12 hours before departure — 25% refund";
        else                 return "Less than 6 hours before departure — No refund";
    }

    // ── Simulated Refund Gateway ──────────────────────────────────────────────

    /**
     * Simulates processing the refund through a payment gateway.
     * Returns a transaction ID on success, throws on failure (rare, 5%).
     */
    public String processRefund(BigDecimal refundAmount) {
        log.info("Simulating refund gateway for amount=Rs.{}", refundAmount);

        // Simulate 95% success rate
        boolean success = new Random().nextInt(100) < 95;

        if (!success) {
            log.warn("Refund gateway simulated failure — will retry");
            throw new RuntimeException("Refund gateway temporarily unavailable");
        }

        String txnId = "REF-TXN-" + System.currentTimeMillis();
        log.info("Refund gateway: SUCCESS — txnId={}, amount=Rs.{}", txnId, refundAmount);
        return txnId;
    }
}
