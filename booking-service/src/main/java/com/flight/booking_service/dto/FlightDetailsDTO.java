package com.flight.booking_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)   // ignore durationMinutes, timestamp, etc.
public class FlightDetailsDTO {
    private Long id;
    private String flightNumber;
    private String airline;
    private String departureCity;
    private String departureAirport;
    private String destinationCity;
    private String destinationAirport;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")  // must match flight-service serialisation format
    private LocalDateTime departureTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime arrivalTime;

    private BigDecimal price;
    private int availableSeats;
    private String status;          // "SCHEDULED", "CANCELLED", etc.
}
