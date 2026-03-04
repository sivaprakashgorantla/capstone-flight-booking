package com.flight.booking_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookingResponse {
    private Long id;
    private String bookingReference;
    private Long flightId;
    private String flightNumber;
    private String airline;
    private String departureCity;
    private String departureAirport;
    private String destinationCity;
    private String destinationAirport;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private String userId;
    private String userEmail;
    private int passengerCount;
    private BigDecimal pricePerSeat;
    private BigDecimal totalAmount;
    private String status;
    private String paymentReference;
    private List<PassengerResponse> passengers;
    private LocalDateTime createdAt;
    private String nextStep;
}
