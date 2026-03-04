package com.flight.reporting.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * UC10 — PAYMENT_REPORT payload.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentReport {

    private String reportReference;
    private String reportType;
    private String generatedAt;

    // ── Headline ──────────────────────────────────────────────────────────────
    private long totalPayments;
    private long successfulPayments;
    private long failedPayments;
    private long pendingPayments;
    private double successRate;             // percentage
    private double failureRate;             // percentage
    private BigDecimal totalRevenue;
    private BigDecimal averagePaymentAmount;
    private BigDecimal largestPayment;
    private BigDecimal smallestPayment;

    /** Payment counts keyed by status */
    private Map<String, Long> byStatus;

    // ── By payment method ─────────────────────────────────────────────────────
    private List<PaymentMethodStat> byMethod;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentMethodStat {
        private String method;
        private long count;
        private BigDecimal totalAmount;
        private double successRate;
    }

    // ── Monthly payment trend ─────────────────────────────────────────────────
    private List<MonthlyPayment> monthlyTrend;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyPayment {
        private String month;
        private long successful;
        private long failed;
        private BigDecimal revenue;
    }

    // ── Failed payment analysis ───────────────────────────────────────────────
    private List<FailureReason> failureReasons;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailureReason {
        private String reason;
        private long count;
        private double percentage;
    }
}
