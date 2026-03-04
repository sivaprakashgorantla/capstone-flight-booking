package com.flight.flight_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Flight search results")
public class FlightSearchResponse {

    @Schema(description = "Departure city searched", example = "Delhi")
    private String departureCity;

    @Schema(description = "Destination city searched", example = "Mumbai")
    private String destinationCity;

    @Schema(description = "Travel date searched", example = "2026-03-10")
    private String travelDate;

    @Schema(description = "Passengers requested", example = "1")
    private int passengers;

    @Schema(description = "Total matching flights found", example = "3")
    private int totalFlights;

    @Schema(description = "List of available flights")
    private List<FlightDTO> flights;
}
