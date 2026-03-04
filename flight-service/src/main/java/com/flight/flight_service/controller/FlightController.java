package com.flight.flight_service.controller;

import com.flight.flight_service.dto.*;
import com.flight.flight_service.model.FlightStatus;
import com.flight.flight_service.service.FlightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/flights")
@RequiredArgsConstructor
@Tag(name = "Flight Search", description = "Search flights and manage flight inventory")
public class FlightController {

    private final FlightService flightService;

    // --- Health -------------------------------------------------------

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Returns service health status - no auth required")
    public ResponseEntity<ApiResponse<String>> health() {
        log.info("Health check called");
        return ResponseEntity.ok(ApiResponse.success("flight-service is up and running"));
    }

    // --- Public: Search -----------------------------------------------

    @GetMapping("/search")
    @Operation(
        summary     = "Search available flights",
        description = "Search flights by departure city, destination, date, and number of passengers. "
                    + "Available to guests (no JWT required) and authenticated users."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Flights found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    public ResponseEntity<ApiResponse<FlightSearchResponse>> searchFlights(
            @Parameter(description = "Departure city", required = true, example = "Delhi")
            @RequestParam String departureCity,

            @Parameter(description = "Destination city", required = true, example = "Mumbai")
            @RequestParam String destinationCity,

            @Parameter(description = "Travel date (yyyy-MM-dd)", required = true, example = "2026-03-10")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate travelDate,

            @Parameter(description = "Number of passengers (default 1)", example = "1")
            @RequestParam(defaultValue = "1") int passengers
    ) {
        log.info("GET /flights/search from={} to={} date={} passengers={}",
                departureCity, destinationCity, travelDate, passengers);

        if (passengers < 1) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Passengers must be at least 1"));
        }

        FlightSearchRequest request = new FlightSearchRequest();
        request.setDepartureCity(departureCity);
        request.setDestinationCity(destinationCity);
        request.setTravelDate(travelDate);
        request.setPassengers(passengers);

        FlightSearchResponse response = flightService.searchFlights(request);

        String message = response.getTotalFlights() > 0
                ? response.getTotalFlights() + " flight(s) found"
                : "No flights found for the selected criteria";

        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    // --- Public: Get by ID -------------------------------------------

    @GetMapping("/{id}")
    @Operation(
        summary     = "Get flight details",
        description = "Get full details for a specific flight by ID - no auth required"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Flight found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Flight not found")
    })
    public ResponseEntity<ApiResponse<FlightDTO>> getFlightById(
            @Parameter(description = "Flight ID", required = true, example = "1")
            @PathVariable Long id) {
        log.info("GET /flights/{}", id);
        FlightDTO flight = flightService.getFlightById(id);
        return ResponseEntity.ok(ApiResponse.success("Flight details retrieved", flight));
    }

    // --- Admin: List All ----------------------------------------------

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary     = "List all flights (ADMIN)",
        description = "Returns all flights regardless of status, ordered by departure time - ADMIN only"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Flights listed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<ApiResponse<List<FlightDTO>>> getAllFlights() {
        log.info("Admin: GET /flights");
        List<FlightDTO> flights = flightService.getAllFlights();
        return ResponseEntity.ok(ApiResponse.success(flights.size() + " flight(s) found", flights));
    }

    // --- Admin: Create -----------------------------------------------

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary     = "Create a new flight (ADMIN)",
        description = "Add a new scheduled flight to the system - ADMIN only"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Flight created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or duplicate flight number"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<ApiResponse<FlightDTO>> createFlight(
            @Valid @RequestBody CreateFlightRequest request) {
        log.info("Admin: POST /flights - {}", request.getFlightNumber());
        FlightDTO created = flightService.createFlight(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Flight created successfully", created));
    }

    // --- Admin: Update Status -----------------------------------------

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary     = "Update flight status (ADMIN)",
        description = "Change flight status: SCHEDULED, DELAYED, CANCELLED, COMPLETED - ADMIN only"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Status updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Flight not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<ApiResponse<FlightDTO>> updateStatus(
            @Parameter(description = "Flight ID", required = true) @PathVariable Long id,
            @Parameter(description = "New status", required = true, example = "DELAYED")
            @RequestParam FlightStatus status) {
        log.info("Admin: PATCH /flights/{}/status -> {}", id, status);
        FlightDTO updated = flightService.updateFlightStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Flight status updated to " + status, updated));
    }

    // --- Admin: Delete -----------------------------------------------

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary     = "Delete a flight (ADMIN)",
        description = "Remove a flight from the system permanently - ADMIN only"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Flight deleted"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Flight not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<ApiResponse<Void>> deleteFlight(
            @Parameter(description = "Flight ID", required = true) @PathVariable Long id) {
        log.info("Admin: DELETE /flights/{}", id);
        flightService.deleteFlight(id);
        return ResponseEntity.ok(ApiResponse.success("Flight deleted successfully"));
    }
}