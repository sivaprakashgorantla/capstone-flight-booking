package com.flight.reporting.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

/**
 * UC10 — FLIGHT_UTILIZATION report payload.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FlightUtilizationReport {

    private String reportReference;
    private String reportType;
    private String generatedAt;

    // ── Summary ───────────────────────────────────────────────────────────────
    private long totalFlights;
    private long totalSeatsAvailable;
    private long totalSeatsFilled;
    private double overallOccupancyRate;   // percentage 0–100

    // ── Per-flight breakdown ──────────────────────────────────────────────────
    private List<FlightStat> flights;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FlightStat {
        private String flightNumber;
        private String airline;
        private String departureCity;
        private String destinationCity;
        private String departureTime;
        private int totalSeats;
        private int seatsBooked;
        private int seatsAvailable;
        private double occupancyRate;      // percentage
        private String utilizationBand;   // LOW / MEDIUM / HIGH / FULL
    }

    // ── Occupancy bands summary ───────────────────────────────────────────────
    private long highUtilizationFlights;    // >= 80%
    private long mediumUtilizationFlights;  // 50–79%
    private long lowUtilizationFlights;     // < 50%
}
