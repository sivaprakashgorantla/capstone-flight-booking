package com.flight.cancellation_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mirrors booking-service BookingResponse — used when calling
 * GET /bookings/internal/{id} via BookingServiceClient.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookingDetailsDTO {

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
    private String status;           // BookingStatus enum name as String
    private String paymentReference;
    private LocalDateTime createdAt;
}
