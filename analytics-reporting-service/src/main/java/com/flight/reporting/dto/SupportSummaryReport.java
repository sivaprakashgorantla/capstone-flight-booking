package com.flight.reporting.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * UC10 — SUPPORT_SUMMARY report payload.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SupportSummaryReport {

    private String reportReference;
    private String reportType;
    private String generatedAt;

    // ── Headline ──────────────────────────────────────────────────────────────
    private long totalTickets;
    private long openTickets;
    private long inProgressTickets;
    private long awaitingUserTickets;
    private long resolvedTickets;
    private long closedTickets;
    private double resolutionRate;         // % resolved or closed
    private double averageResolutionHours; // avg time to resolve

    /** Ticket counts keyed by status */
    private Map<String, Long> byStatus;

    // ── By category ───────────────────────────────────────────────────────────
    private List<CategoryStat> byCategory;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryStat {
        private String category;
        private long total;
        private long resolved;
        private double resolutionRate;
        private double avgResolutionHours;
    }

    // ── By priority ───────────────────────────────────────────────────────────
    private Map<String, Long> byPriority;

    // ── Agent performance ─────────────────────────────────────────────────────
    private List<AgentStat> agentPerformance;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgentStat {
        private String agentName;
        private long assigned;
        private long resolved;
        private double resolutionRate;
    }
}
