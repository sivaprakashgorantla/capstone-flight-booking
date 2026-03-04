package com.flight.flight_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "Request body for creating a new flight (ADMIN only)")
public class CreateFlightRequest {

    @NotBlank(message = "Flight number is required")
    @Size(max = 20, message = "Flight number must be at most 20 characters")
    @Schema(description = "Unique flight number", example = "AI-201")
    private String flightNumber;

    @NotBlank(message = "Airline name is required")
    @Schema(description = "Airline name", example = "Air India")
    private String airline;

    @NotBlank(message = "Departure city is required")
    @Schema(description = "Departure city", example = "Bengaluru")
    private String departureCity;

    @NotBlank(message = "Departure airport code is required")
    @Size(min = 3, max = 4, message = "Airport code must be 3-4 characters (IATA)")
    @Schema(description = "Departure IATA airport code", example = "BLR")
    private String departureAirport;

    @NotBlank(message = "Destination city is required")
    @Schema(description = "Destination city", example = "Chennai")
    private String destinationCity;

    @NotBlank(message = "Destination airport code is required")
    @Size(min = 3, max = 4, message = "Airport code must be 3-4 characters (IATA)")
    @Schema(description = "Destination IATA airport code", example = "MAA")
    private String destinationAirport;

    @NotNull(message = "Departure time is required")
    @Future(message = "Departure time must be in the future")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    @Schema(description = "Departure date and time", example = "2026-03-10 07:00")
    private LocalDateTime departureTime;

    @NotNull(message = "Arrival time is required")
    @Future(message = "Arrival time must be in the future")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    @Schema(description = "Arrival date and time", example = "2026-03-10 09:15")
    private LocalDateTime arrivalTime;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Schema(description = "Ticket price in INR", example = "5200.00")
    private BigDecimal price;

    @Min(value = 1, message = "Total seats must be at least 1")
    @Schema(description = "Total seats on this flight", example = "180")
    private int totalSeats;
}
