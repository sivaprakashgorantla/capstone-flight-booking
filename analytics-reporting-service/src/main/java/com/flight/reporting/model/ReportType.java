package com.flight.reporting.model;

/**
 * UC10 Step 1 — Admin selects one of these report types.
 */
public enum ReportType {
    BOOKING_SUMMARY,        // Total bookings by status, top routes, occupancy
    REVENUE_REPORT,         // Revenue by month, by route, average ticket price
    FLIGHT_UTILIZATION,     // Per-flight occupancy rates and seat fill %
    CANCELLATION_REPORT,    // Cancellation counts, refund amounts, by reason
    SUPPORT_SUMMARY,        // Support ticket metrics by category and status
    PAYMENT_REPORT,         // Payment success/failure rates and totals
    FULL_DASHBOARD          // All metrics combined (UC10 Step 4 — view)
}
