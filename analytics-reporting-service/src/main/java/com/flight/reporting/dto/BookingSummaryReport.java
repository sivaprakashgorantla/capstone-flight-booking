package com.flight.reporting.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * UC10 — BOOKING_SUMMARY report payload.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookingSummaryReport {

    private String reportReference;
    private String reportType;
    private String generatedAt;

    // ── Headline metrics ──────────────────────────────────────────────────────
    private long totalBookings;
    private long confirmedBookings;
    private long cancelledBookings;
    private long pendingPaymentBookings;
    private long paymentFailedBookings;
    private BigDecimal totalRevenue;
    private BigDecimal averageBookingValue;

    /** Booking counts keyed by status name */
    private Map<String, Long> bookingsByStatus;

    // ── Route analytics ───────────────────────────────────────────────────────
    private List<RouteStats> topRoutes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RouteStats {
        private String route;
        private String departureCity;
        private String destinationCity;
        private long bookingCount;
        private BigDecimal revenue;
    }

    // ── Monthly trend ─────────────────────────────────────────────────────────
    private List<MonthlyBooking> monthlyTrend;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyBooking {
        private String month;   // e.g. "Jan 2026"
        private long bookings;
        private BigDecimal revenue;
    }
}
