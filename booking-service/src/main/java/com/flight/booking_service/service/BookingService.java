package com.flight.booking_service.service;

import com.flight.booking_service.dto.*;
import com.flight.booking_service.cqrs.CommandHandler;
import com.flight.booking_service.cqrs.QueryHandler;
import com.flight.booking_service.exception.BadRequestException;
import com.flight.booking_service.exception.BookingNotFoundException;
import com.flight.booking_service.saga.BookingSagaOrchestrator;
import org.springframework.security.access.AccessDeniedException;
import com.flight.booking_service.model.*;
import com.flight.booking_service.repository.BookingRepository;
import com.flight.booking_service.repository.PassengerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final PassengerRepository passengerRepository;
    private final FlightServiceClient flightServiceClient;
    private final EmailService emailService;
    private final BookingSagaOrchestrator sagaOrchestrator;

    // ── Step 1: Create Booking ─────────────────────────────────────────────────

    @CommandHandler("CreateBooking")
    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request,
                                         String username, String userEmail) {
        log.info("Creating booking for user={}, flightId={}, passengers={}",
                username, request.getFlightId(), request.getPassengers().size());

        // 1. Validate flight
        FlightDetailsDTO flight = flightServiceClient.getFlightDetails(request.getFlightId());

        if (!"SCHEDULED".equals(flight.getStatus())) {
            throw new BadRequestException("Flight " + flight.getFlightNumber()
                    + " is not available for booking (status: " + flight.getStatus() + ")");
        }
        if (flight.getAvailableSeats() < request.getPassengers().size()) {
            throw new BadRequestException("Not enough seats. Available: "
                    + flight.getAvailableSeats()
                    + ", Requested: " + request.getPassengers().size());
        }

        // 2. Build booking
        String bookingRef    = generateReference("BKG");
        BigDecimal totalAmount = flight.getPrice()
                .multiply(BigDecimal.valueOf(request.getPassengers().size()));

        Booking booking = Booking.builder()
                .bookingReference(bookingRef)
                .flightId(flight.getId())
                .flightNumber(flight.getFlightNumber())
                .airline(flight.getAirline())
                .departureCity(flight.getDepartureCity())
                .departureAirport(flight.getDepartureAirport())
                .destinationCity(flight.getDestinationCity())
                .destinationAirport(flight.getDestinationAirport())
                .departureTime(flight.getDepartureTime())
                .arrivalTime(flight.getArrivalTime())
                .userId(username)
                .userEmail(userEmail)
                .passengerCount(request.getPassengers().size())
                .pricePerSeat(flight.getPrice())
                .totalAmount(totalAmount)
                .status(BookingStatus.PENDING_PAYMENT)
                .passengers(new ArrayList<>())
                .build();

        // 3. Assign seats & build passengers
        Set<String> takenSeats = getTakenSeatsForFlight(request.getFlightId());
        List<Passenger> passengers = new ArrayList<>();

        for (PassengerRequest pr : request.getPassengers()) {
            String seat = assignSeat(takenSeats); // also adds seat to takenSeats
            passengers.add(Passenger.builder()
                    .booking(booking)
                    .firstName(pr.getFirstName())
                    .lastName(pr.getLastName())
                    .email(pr.getEmail())
                    .phone(pr.getPhone())
                    .age(pr.getAge())
                    .gender(pr.getGender())
                    .seatNumber(seat)
                    .passportNumber(pr.getPassportNumber())
                    .build());
        }
        booking.setPassengers(passengers);

        // 4. Persist
        Booking saved = bookingRepository.save(booking);
        log.info("Booking created: ref={}, user={}, amount={}",
                bookingRef, username, totalAmount);

        // SAGA STEP 1 — booking created, awaiting payment
        sagaOrchestrator.onBookingCreated(
                bookingRef, username,
                request.getPassengers().size(),
                totalAmount.toPlainString());

        BookingResponse response = toResponse(saved);
        response.setNextStep("Proceed to POST /payments/initiate  |  bookingId="
                + saved.getId() + "  |  bookingReference=" + bookingRef
                + "  |  totalAmount=" + totalAmount);
        return response;
    }

    // ── Confirm / Fail Booking (internal — called by payment-service) ──────────

    @CommandHandler("ConfirmOrFailBooking")
    @Transactional
    public BookingResponse confirmBooking(ConfirmBookingRequest request) {
        log.info("Confirming booking: ref={}, payment={}, status={}",
                request.getBookingReference(),
                request.getPaymentReference(),
                request.getStatus());

        Booking booking = bookingRepository
                .findByBookingReference(request.getBookingReference())
                .orElseThrow(() -> new BookingNotFoundException(
                        "Booking not found: " + request.getBookingReference()));

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            log.warn("Booking {} already CONFIRMED — skipping duplicate callback",
                    request.getBookingReference());
            return toResponse(booking);
        }

        boolean isConfirmed = "CONFIRMED".equalsIgnoreCase(request.getStatus());
        booking.setStatus(isConfirmed ? BookingStatus.CONFIRMED : BookingStatus.PAYMENT_FAILED);
        booking.setPaymentReference(request.getPaymentReference());
        Booking saved = bookingRepository.save(booking);

        log.info("Booking {} updated to {}", saved.getBookingReference(), saved.getStatus());

        // SAGA STEP 3 — happy path or compensating transaction
        if (isConfirmed) {
            sagaOrchestrator.onSagaCompleted(
                    request.getBookingReference(), request.getPaymentReference());
        } else {
            sagaOrchestrator.onSagaCompensated(
                    request.getBookingReference(), request.getPaymentReference(),
                    "Payment processing failed");
        }

        List<Passenger> passengers = saved.getPassengers();
        if (isConfirmed) {
            emailService.sendBookingConfirmation(saved, passengers);
        } else {
            emailService.sendPaymentFailureNotification(saved, "Payment processing failed");
        }
        return toResponse(saved);
    }

    // ── Get My Bookings ────────────────────────────────────────────────────────

    @QueryHandler("GetMyBookings")
    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(String username) {
        log.info("Fetching bookings for user={}", username);
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(username)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Get by ID ──────────────────────────────────────────────────────────────

    @QueryHandler("GetBookingById")
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long id, String username) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException(
                        "Booking not found with id: " + id));
        if (!booking.getUserId().equals(username)) {
            throw new AccessDeniedException("Access denied to booking id: " + id);
        }
        return toResponse(booking);
    }

    // ── Get by Reference ───────────────────────────────────────────────────────

    @QueryHandler("GetBookingByReference")
    @Transactional(readOnly = true)
    public BookingResponse getBookingByReference(String ref, String username) {
        Booking booking = bookingRepository.findByBookingReference(ref)
                .orElseThrow(() -> new BookingNotFoundException(
                        "Booking not found: " + ref));
        if (!booking.getUserId().equals(username)) {
            throw new AccessDeniedException("Access denied to booking: " + ref);
        }
        return toResponse(booking);
    }

    // ── Cancel Booking ─────────────────────────────────────────────────────────

    @CommandHandler("CancelBooking")
    @Transactional
    public BookingResponse cancelBooking(Long id, String username) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException(
                        "Booking not found with id: " + id));
        if (!booking.getUserId().equals(username)) {
            throw new AccessDeniedException("Access denied to booking id: " + id);
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Booking is already cancelled.");
        }
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            throw new BadRequestException(
                    "CONFIRMED bookings cannot be cancelled directly. "
                    + "Please contact support for a refund.");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        Booking saved = bookingRepository.save(booking);
        log.info("Booking {} cancelled by user={}", saved.getBookingReference(), username);
        return toResponse(saved);
    }

    // ── Internal: get booking details (called by cancellation-service) ────────

    @QueryHandler("GetBookingByIdInternal")
    @Transactional(readOnly = true)
    public BookingResponse getBookingByIdInternal(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException(
                        "Booking not found with id: " + id));
        return toResponse(booking);
    }

    // ── Internal: cancel booking (called by cancellation-service) ─────────────

    @CommandHandler("CancelBookingInternal")
    @Transactional
    public void cancelBookingInternal(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException(
                        "Booking not found with id: " + id));
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            log.warn("Booking {} is already CANCELLED — skipping internal cancel", id);
            return;
        }
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        log.info("Booking {} cancelled via internal endpoint", id);
    }

    // ── Admin: All Bookings ────────────────────────────────────────────────────

    @QueryHandler("GetAllBookings")
    @Transactional(readOnly = true)
    public List<BookingResponse> getAllBookings() {
        return bookingRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Private Helpers ────────────────────────────────────────────────────────

    /**
     * Collect all seat numbers already taken for the given flight
     * across active bookings (PENDING_PAYMENT, PAYMENT_PROCESSING, CONFIRMED).
     */
    private Set<String> getTakenSeatsForFlight(Long flightId) {
        List<BookingStatus> activeStatuses = List.of(
                BookingStatus.PENDING_PAYMENT,
                BookingStatus.PAYMENT_PROCESSING,
                BookingStatus.CONFIRMED
        );
        return bookingRepository
                .findByFlightIdAndStatusIn(flightId, activeStatuses)
                .stream()
                .flatMap(b -> b.getPassengers().stream())
                .map(Passenger::getSeatNumber)
                .collect(Collectors.toSet());
    }

    /**
     * Returns the first free seat (1A … 50F) and marks it as taken.
     */
    private String assignSeat(Set<String> takenSeats) {
        String[] cols = {"A", "B", "C", "D", "E", "F"};
        for (int row = 1; row <= 50; row++) {
            for (String col : cols) {
                String seat = row + col;
                if (!takenSeats.contains(seat)) {
                    takenSeats.add(seat); // reserve for subsequent passengers
                    return seat;
                }
            }
        }
        throw new BadRequestException("No seats available on this flight.");
    }

    // ════════════════════════════════════════════════════════════════════════════
    // USE CASE 4 — Manage Bookings
    // ════════════════════════════════════════════════════════════════════════════

    // ── Step 1: View passenger list for a booking ──────────────────────────────

    @QueryHandler("GetPassengers")
    @Transactional(readOnly = true)
    public List<PassengerResponse> getPassengers(Long bookingId, String username) {
        log.info("Fetching passengers for bookingId={}, user={}", bookingId, username);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(
                        "Booking not found with id: " + bookingId));
        if (!booking.getUserId().equals(username)) {
            throw new AccessDeniedException("Access denied to booking id: " + bookingId);
        }
        return passengerRepository.findByBookingId(bookingId)
                .stream()
                .map(this::toPassengerResponse)
                .collect(Collectors.toList());
    }

    // ── Step 2: Modify — update a single passenger's details ──────────────────

    @CommandHandler("UpdatePassenger")
    @Transactional
    public PassengerResponse updatePassenger(Long bookingId,
                                             Long passengerId,
                                             UpdatePassengerRequest req,
                                             String username) {
        log.info("Updating passenger id={} on bookingId={} by user={}",
                passengerId, bookingId, username);

        // Ownership check
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(
                        "Booking not found with id: " + bookingId));
        if (!booking.getUserId().equals(username)) {
            throw new AccessDeniedException("Access denied to booking id: " + bookingId);
        }

        // Only allow modification on PENDING_PAYMENT bookings
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BadRequestException(
                    "Passenger details can only be modified when booking is in PENDING_PAYMENT status. "
                    + "Current status: " + booking.getStatus());
        }

        // Passenger must belong to this booking
        Passenger passenger = passengerRepository
                .findByIdAndBookingId(passengerId, bookingId)
                .orElseThrow(() -> new BookingNotFoundException(
                        "Passenger id=" + passengerId
                        + " not found in booking id=" + bookingId));

        // Apply changes
        passenger.setFirstName(req.getFirstName());
        passenger.setLastName(req.getLastName());
        passenger.setEmail(req.getEmail());
        passenger.setPhone(req.getPhone());
        passenger.setGender(req.getGender());
        passenger.setPassportNumber(req.getPassportNumber());

        Passenger saved = passengerRepository.save(passenger);
        log.info("Passenger id={} updated — name={} {}",
                saved.getId(), saved.getFirstName(), saved.getLastName());

        return toPassengerResponse(saved);
    }

    // ── Step 3: Cancel booking (re-exposed with richer logging) ───────────────
    // NOTE: cancelBooking() already exists above — no duplication needed.

    // ── Admin: Update booking status ──────────────────────────────────────────

    @CommandHandler("UpdateBookingStatus")
    @Transactional
    public BookingResponse updateBookingStatus(Long id, UpdateBookingStatusRequest req) {
        log.info("ADMIN status update — bookingId={}, newStatus={}, reason={}",
                id, req.getStatus(), req.getReason());

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException(
                        "Booking not found with id: " + id));

        BookingStatus newStatus;
        try {
            newStatus = BookingStatus.valueOf(req.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status value: " + req.getStatus()
                    + ". Allowed: PENDING_PAYMENT, PAYMENT_PROCESSING, CONFIRMED, PAYMENT_FAILED, CANCELLED");
        }

        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(newStatus);
        Booking saved = bookingRepository.save(booking);
        log.info("ADMIN: booking {} status changed {} → {}", saved.getBookingReference(),
                oldStatus, newStatus);

        return toResponse(saved);
    }

    // ── Admin: Booking statistics ─────────────────────────────────────────────

    @QueryHandler("GetBookingStats")
    @Transactional(readOnly = true)
    public BookingStatsResponse getBookingStats() {
        log.info("Generating booking statistics");
        List<Booking> all = bookingRepository.findAll();

        long pending    = all.stream().filter(b -> b.getStatus() == BookingStatus.PENDING_PAYMENT).count();
        long processing = all.stream().filter(b -> b.getStatus() == BookingStatus.PAYMENT_PROCESSING).count();
        long confirmed  = all.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED).count();
        long failed     = all.stream().filter(b -> b.getStatus() == BookingStatus.PAYMENT_FAILED).count();
        long cancelled  = all.stream().filter(b -> b.getStatus() == BookingStatus.CANCELLED).count();

        BigDecimal revenue = all.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .map(Booking::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return BookingStatsResponse.builder()
                .total((long) all.size())
                .pendingPayment(pending)
                .paymentProcessing(processing)
                .confirmed(confirmed)
                .paymentFailed(failed)
                .cancelled(cancelled)
                .totalRevenue(revenue)
                .build();
    }

    // ── Private: passenger → DTO ──────────────────────────────────────────────

    private PassengerResponse toPassengerResponse(Passenger p) {
        return PassengerResponse.builder()
                .id(p.getId())
                .firstName(p.getFirstName())
                .lastName(p.getLastName())
                .email(p.getEmail())
                .phone(p.getPhone())
                .age(p.getAge())
                .gender(p.getGender())
                .seatNumber(p.getSeatNumber())
                .passportNumber(p.getPassportNumber())
                .build();
    }

    /**
     * Generates a unique reference like BKG-A1B2C3D4 (collision-safe, recursive).
     */
    private String generateReference(String prefix) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(prefix + "-");
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        String ref = sb.toString();
        return bookingRepository.existsByBookingReference(ref)
                ? generateReference(prefix)
                : ref;
    }

    /**
     * Maps Booking entity → BookingResponse DTO.
     */
    private BookingResponse toResponse(Booking b) {
        List<PassengerResponse> passengerResponses = (b.getPassengers() == null)
                ? Collections.emptyList()
                : b.getPassengers().stream()
                        .map(p -> PassengerResponse.builder()
                                .id(p.getId())
                                .firstName(p.getFirstName())
                                .lastName(p.getLastName())
                                .email(p.getEmail())
                                .phone(p.getPhone())
                                .age(p.getAge())
                                .gender(p.getGender())
                                .seatNumber(p.getSeatNumber())
                                .passportNumber(p.getPassportNumber())
                                .build())
                        .collect(Collectors.toList());

        return BookingResponse.builder()
                .id(b.getId())
                .bookingReference(b.getBookingReference())
                .flightId(b.getFlightId())
                .flightNumber(b.getFlightNumber())
                .airline(b.getAirline())
                .departureCity(b.getDepartureCity())
                .departureAirport(b.getDepartureAirport())
                .destinationCity(b.getDestinationCity())
                .destinationAirport(b.getDestinationAirport())
                .departureTime(b.getDepartureTime())
                .arrivalTime(b.getArrivalTime())
                .userId(b.getUserId())
                .userEmail(b.getUserEmail())
                .passengerCount(b.getPassengerCount())
                .pricePerSeat(b.getPricePerSeat())
                .totalAmount(b.getTotalAmount())
                .status(b.getStatus().name())
                .paymentReference(b.getPaymentReference())
                .passengers(passengerResponses)
                .createdAt(b.getCreatedAt())
                .build();
    }
}
