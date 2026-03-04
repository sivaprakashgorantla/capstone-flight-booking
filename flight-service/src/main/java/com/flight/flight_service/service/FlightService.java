package com.flight.flight_service.service;

import com.flight.flight_service.dto.*;
import com.flight.flight_service.exception.FlightNotFoundException;
import com.flight.flight_service.model.Flight;
import com.flight.flight_service.model.FlightStatus;
import com.flight.flight_service.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlightService {

    private final FlightRepository flightRepository;

    // ─── Search (Public / Guest) ──────────────────────────────────────────────

    /**
     * Search available flights by city pair, date, and passenger count.
     */
    @Transactional(readOnly = true)
    public FlightSearchResponse searchFlights(FlightSearchRequest request) {
        log.info("Searching flights: {} → {} on {} for {} passenger(s)",
                request.getDepartureCity(), request.getDestinationCity(),
                request.getTravelDate(), request.getPassengers());

        LocalDateTime startOfDay = request.getTravelDate().atStartOfDay();
        LocalDateTime endOfDay   = request.getTravelDate().atTime(LocalTime.MAX);

        List<Flight> flights = flightRepository.searchFlights(
                request.getDepartureCity(),
                request.getDestinationCity(),
                startOfDay,
                endOfDay,
                request.getPassengers(),
                FlightStatus.SCHEDULED
        );

        log.info("Found {} flight(s) matching search criteria", flights.size());

        List<FlightDTO> flightDTOs = flights.stream()
                .map(this::toFlightDTO)
                .toList();

        return FlightSearchResponse.builder()
                .departureCity(request.getDepartureCity())
                .destinationCity(request.getDestinationCity())
                .travelDate(request.getTravelDate().toString())
                .passengers(request.getPassengers())
                .totalFlights(flightDTOs.size())
                .flights(flightDTOs)
                .build();
    }

    // ─── Get by ID (Public) ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public FlightDTO getFlightById(Long id) {
        log.info("Fetching flight with id={}", id);
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new FlightNotFoundException("Flight not found with id: " + id));
        return toFlightDTO(flight);
    }

    // ─── Admin: List All ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<FlightDTO> getAllFlights() {
        log.info("Admin: fetching all flights");
        return flightRepository.findAllByOrderByDepartureTimeAsc()
                .stream().map(this::toFlightDTO).toList();
    }

    // ─── Admin: Create ────────────────────────────────────────────────────────

    @Transactional
    public FlightDTO createFlight(CreateFlightRequest request) {
        log.info("Admin: creating flight {}", request.getFlightNumber());

        if (flightRepository.existsByFlightNumber(request.getFlightNumber())) {
            throw new IllegalArgumentException(
                    "Flight number already exists: " + request.getFlightNumber());
        }

        if (!request.getArrivalTime().isAfter(request.getDepartureTime())) {
            throw new IllegalArgumentException("Arrival time must be after departure time");
        }

        Flight flight = Flight.builder()
                .flightNumber(request.getFlightNumber())
                .airline(request.getAirline())
                .departureCity(request.getDepartureCity())
                .departureAirport(request.getDepartureAirport().toUpperCase())
                .destinationCity(request.getDestinationCity())
                .destinationAirport(request.getDestinationAirport().toUpperCase())
                .departureTime(request.getDepartureTime())
                .arrivalTime(request.getArrivalTime())
                .price(request.getPrice())
                .totalSeats(request.getTotalSeats())
                .availableSeats(request.getTotalSeats())
                .status(FlightStatus.SCHEDULED)
                .build();

        Flight saved = flightRepository.save(flight);
        log.info("Flight created: {} (id={})", saved.getFlightNumber(), saved.getId());
        return toFlightDTO(saved);
    }

    // ─── Admin: Update Status ─────────────────────────────────────────────────

    @Transactional
    public FlightDTO updateFlightStatus(Long id, FlightStatus newStatus) {
        log.info("Admin: updating flight id={} status → {}", id, newStatus);
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new FlightNotFoundException("Flight not found with id: " + id));
        flight.setStatus(newStatus);
        return toFlightDTO(flightRepository.save(flight));
    }

    // ─── Admin: Delete ────────────────────────────────────────────────────────

    @Transactional
    public void deleteFlight(Long id) {
        log.info("Admin: deleting flight id={}", id);
        if (!flightRepository.existsById(id)) {
            throw new FlightNotFoundException("Flight not found with id: " + id);
        }
        flightRepository.deleteById(id);
    }

    // ─── Mapper ───────────────────────────────────────────────────────────────

    private FlightDTO toFlightDTO(Flight f) {
        long durationMinutes = ChronoUnit.MINUTES.between(f.getDepartureTime(), f.getArrivalTime());
        return FlightDTO.builder()
                .id(f.getId())
                .flightNumber(f.getFlightNumber())
                .airline(f.getAirline())
                .departureCity(f.getDepartureCity())
                .departureAirport(f.getDepartureAirport())
                .destinationCity(f.getDestinationCity())
                .destinationAirport(f.getDestinationAirport())
                .departureTime(f.getDepartureTime())
                .arrivalTime(f.getArrivalTime())
                .durationMinutes(durationMinutes)
                .price(f.getPrice())
                .availableSeats(f.getAvailableSeats())
                .status(f.getStatus())
                .build();
    }
}
