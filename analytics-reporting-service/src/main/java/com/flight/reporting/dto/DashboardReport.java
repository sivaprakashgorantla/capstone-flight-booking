package com.flight.reporting.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;

/**
 * UC10 Step 4 — FULL_DASHBOARD: all KPIs on one page.
 * Embeds the headline figures from each individual report.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardReport {

    private String reportReference;
    private String reportType;
    private String generatedAt;

    // ── Booking KPIs ─────────────────────────────────────────────────────────
    private long totalBookings;
    private long confirmedBookings;
    private long cancelledBookings;
    private BigDecimal totalBookingRevenue;

    // ── Revenue KPIs ─────────────────────────────────────────────────────────
    private BigDecimal totalRevenue;
    private BigDecimal totalRefunds;
    private BigDecimal netRevenue;
    private BigDecimal averageTicketPrice;

    // ── Flight KPIs ───────────────────────────────────────────────────────────
    private long totalFlights;
    private double overallOccupancyRate;
    private long highUtilizationFlights;
    private long lowUtilizationFlights;

    // ── Cancellation KPIs ────────────────────────────────────────────────────
    private long totalCancellations;
    private BigDecimal totalRefundAmount;
    private double cancellationRate;        // % of bookings cancelled

    // ── Support KPIs ─────────────────────────────────────────────────────────
    private long totalSupportTickets;
    private long openSupportTickets;
    private double ticketResolutionRate;
    private double averageResolutionHours;

    // ── Payment KPIs ─────────────────────────────────────────────────────────
    private long totalPayments;
    private double paymentSuccessRate;
    private long failedPayments;

    // ── Drill-down links ─────────────────────────────────────────────────────
    private String bookingDetailEndpoint;
    private String revenueDetailEndpoint;
    private String flightDetailEndpoint;
    private String cancellationDetailEndpoint;
    private String supportDetailEndpoint;
    private String paymentDetailEndpoint;
}
