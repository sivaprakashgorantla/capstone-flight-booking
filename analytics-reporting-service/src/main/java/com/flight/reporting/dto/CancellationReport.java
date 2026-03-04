package com.flight.reporting.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * UC10 — CANCELLATION_REPORT payload.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CancellationReport {

    private String reportReference;
    private String reportType;
    private String generatedAt;

    // ── Headline ──────────────────────────────────────────────────────────────
    private long totalCancellations;
    private long refundedCancellations;
    private long noRefundCancellations;
    private long rejectedCancellations;
    private BigDecimal totalRefundAmount;
    private BigDecimal totalOriginalAmount;
    private double refundRate;              // % of cancellations that got a refund
    private double averageRefundPercentage; // avg % of ticket value refunded

    /** Cancellation counts keyed by status */
    private Map<String, Long> byStatus;

    // ── Cancellations by reason ───────────────────────────────────────────────
    private List<CancellationReason> byReason;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CancellationReason {
        private String reason;
        private long count;
        private BigDecimal totalRefund;
    }

    // ── Refund policy breakdown ───────────────────────────────────────────────
    private List<RefundBand> refundByTimeBand;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundBand {
        private String band;          // e.g. ">48h (90%)", "24-48h (75%)"
        private long count;
        private BigDecimal refundAmount;
    }

    // ── Monthly trend ─────────────────────────────────────────────────────────
    private List<MonthlyCancellation> monthlyTrend;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyCancellation {
        private String month;
        private long cancellations;
        private BigDecimal refunds;
    }
}
