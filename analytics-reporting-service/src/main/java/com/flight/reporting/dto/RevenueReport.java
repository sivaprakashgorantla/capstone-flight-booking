package com.flight.reporting.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * UC10 — REVENUE_REPORT payload.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RevenueReport {

    private String reportReference;
    private String reportType;
    private String generatedAt;
    private String fromDate;
    private String toDate;

    // ── Headline ──────────────────────────────────────────────────────────────
    private BigDecimal totalRevenue;
    private BigDecimal totalRefunds;
    private BigDecimal netRevenue;
    private BigDecimal averageTicketPrice;
    private long totalTransactions;

    // ── Monthly breakdown ─────────────────────────────────────────────────────
    private List<MonthlyRevenue> monthlyBreakdown;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyRevenue {
        private String month;
        private BigDecimal revenue;
        private BigDecimal refunds;
        private BigDecimal net;
        private long transactions;
    }

    // ── Revenue by route ──────────────────────────────────────────────────────
    private List<RouteRevenue> revenueByRoute;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RouteRevenue {
        private String route;
        private BigDecimal revenue;
        private long tickets;
        private BigDecimal averagePrice;
    }

    // ── Revenue by airline ────────────────────────────────────────────────────
    private List<AirlineRevenue> revenueByAirline;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AirlineRevenue {
        private String airline;
        private BigDecimal revenue;
        private long bookings;
    }
}
