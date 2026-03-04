package com.flight.booking_service.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = "passengers")
@ToString(exclude = "passengers")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_reference", nullable = false, unique = true, length = 15)
    private String bookingReference;

    @Column(name = "flight_id", nullable = false)
    private Long flightId;

    @Column(name = "flight_number", nullable = false, length = 20)
    private String flightNumber;

    @Column(nullable = false, length = 100)
    private String airline;

    @Column(name = "departure_city", nullable = false, length = 100)
    private String departureCity;

    @Column(name = "departure_airport", nullable = false, length = 10)
    private String departureAirport;

    @Column(name = "destination_city", nullable = false, length = 100)
    private String destinationCity;

    @Column(name = "destination_airport", nullable = false, length = 10)
    private String destinationAirport;

    @Column(name = "departure_time", nullable = false)
    private LocalDateTime departureTime;

    @Column(name = "arrival_time", nullable = false)
    private LocalDateTime arrivalTime;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "user_email", nullable = false, length = 150)
    private String userEmail;

    @Column(name = "passenger_count", nullable = false)
    private int passengerCount;

    @Column(name = "price_per_seat", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerSeat;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private BookingStatus status;

    @Column(name = "payment_reference", length = 20)
    private String paymentReference;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Passenger> passengers = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
