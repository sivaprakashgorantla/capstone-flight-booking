package com.flight.flight_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@Schema(description = "Flight search criteria")
public class FlightSearchRequest {

    @NotBlank(message = "Departure city is required")
    @Schema(description = "Departure city name", example = "Delhi")
    private String departureCity;

    @NotBlank(message = "Destination city is required")
    @Schema(description = "Destination city name", example = "Mumbai")
    private String destinationCity;

    @NotNull(message = "Travel date is required")
    @FutureOrPresent(message = "Travel date must be today or in the future")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(description = "Travel date (yyyy-MM-dd)", example = "2026-03-10")
    private LocalDate travelDate;

    @Min(value = 1, message = "At least 1 passenger required")
    @Schema(description = "Number of passengers (default 1)", example = "1")
    private int passengers = 1;
}
