package com.flight.flight_service.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "flights")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Flight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flight_number", nullable = false, unique = true, length = 20)
    private String flightNumber;

    @Column(nullable = false, length = 100)
    private String airline;

    @Column(name = "departure_city", nullable = false, length = 100)
    private String departureCity;

    @Column(name = "departure_airport", nullable = false, length = 10)
    private String departureAirport;   // IATA code, e.g. DEL

    @Column(name = "destination_city", nullable = false, length = 100)
    private String destinationCity;

    @Column(name = "destination_airport", nullable = false, length = 10)
    private String destinationAirport; // IATA code, e.g. BOM

    @Column(name = "departure_time", nullable = false)
    private LocalDateTime departureTime;

    @Column(name = "arrival_time", nullable = false)
    private LocalDateTime arrivalTime;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "total_seats", nullable = false)
    private int totalSeats;

    @Column(name = "available_seats", nullable = false)
    private int availableSeats;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private FlightStatus status = FlightStatus.SCHEDULED;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
