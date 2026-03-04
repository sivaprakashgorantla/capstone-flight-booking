package com.flight.booking_service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlightDetailsDTO {
    private Long id;
    private String flightNumber;
    private String airline;
    private String departureCity;
    private String departureAirport;
    private String destinationCity;
    private String destinationAirport;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private BigDecimal price;
    private int availableSeats;
    private String status;
}
