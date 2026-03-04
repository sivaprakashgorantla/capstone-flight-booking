package com.flight.flight_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.flight.flight_service.model.FlightStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Flight details")
public class FlightDTO {

    @Schema(description = "Internal flight ID", example = "1")
    private Long id;

    @Schema(description = "Flight number", example = "AI-101")
    private String flightNumber;

    @Schema(description = "Airline name", example = "Air India")
    private String airline;

    @Schema(description = "Departure city", example = "Delhi")
    private String departureCity;

    @Schema(description = "Departure airport IATA code", example = "DEL")
    private String departureAirport;

    @Schema(description = "Destination city", example = "Mumbai")
    private String destinationCity;

    @Schema(description = "Destination airport IATA code", example = "BOM")
    private String destinationAirport;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    @Schema(description = "Departure date and time", example = "2026-03-10 06:30")
    private LocalDateTime departureTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    @Schema(description = "Arrival date and time", example = "2026-03-10 08:45")
    private LocalDateTime arrivalTime;

    @Schema(description = "Duration in minutes", example = "135")
    private long durationMinutes;

    @Schema(description = "Ticket price (INR)", example = "4500.00")
    private BigDecimal price;

    @Schema(description = "Available seats", example = "42")
    private int availableSeats;

    @Schema(description = "Flight status", example = "SCHEDULED")
    private FlightStatus status;
}
