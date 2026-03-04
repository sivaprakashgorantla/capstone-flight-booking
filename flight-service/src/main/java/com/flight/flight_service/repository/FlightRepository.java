package com.flight.flight_service.repository;

import com.flight.flight_service.model.Flight;
import com.flight.flight_service.model.FlightStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FlightRepository extends JpaRepository<Flight, Long> {

    boolean existsByFlightNumber(String flightNumber);

    /**
     * Search flights by departure city, destination city, date window, available seats, and status.
     * The date window covers the full day of the requested travel date.
     */
    @Query("""
            SELECT f FROM Flight f
            WHERE LOWER(f.departureCity)   = LOWER(:departureCity)
              AND LOWER(f.destinationCity) = LOWER(:destinationCity)
              AND f.departureTime          >= :startOfDay
              AND f.departureTime          <  :endOfDay
              AND f.availableSeats         >= :passengers
              AND f.status                 = :status
            ORDER BY f.departureTime ASC
            """)
    List<Flight> searchFlights(
            @Param("departureCity")   String departureCity,
            @Param("destinationCity") String destinationCity,
            @Param("startOfDay")      LocalDateTime startOfDay,
            @Param("endOfDay")        LocalDateTime endOfDay,
            @Param("passengers")      int passengers,
            @Param("status")          FlightStatus status
    );

    /** All flights for admin listing. */
    List<Flight> findAllByOrderByDepartureTimeAsc();

    /** Flights by status. */
    List<Flight> findByStatusOrderByDepartureTimeAsc(FlightStatus status);
}
