package com.flight.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookingInfo {
    private Long id;
    private String bookingReference;
    private String flightNumber;
    private String airline;
    private String departureCity;
    private String departureAirport;
    private String destinationCity;
    private String destinationAirport;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private int passengerCount;
    private BigDecimal totalAmount;
    private String status;
    private String paymentReference;
    private LocalDateTime createdAt;
}
