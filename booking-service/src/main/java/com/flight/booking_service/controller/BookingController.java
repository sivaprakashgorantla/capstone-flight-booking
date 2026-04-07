package com.flight.booking_service.controller;

import com.flight.booking_service.dto.*;
import com.flight.booking_service.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Tag(
    name        = "Booking Management",
    description = "Use Case 3 — Create & pay for bookings. "
                + "Use Case 4 — View, modify passenger details, cancel, and admin manage bookings."
)
public class BookingController {

    private final BookingService bookingService;

    @Value("${internal.service.key}")
    private String internalServiceKey;

    // ══════════════════════════════════════════════════════════════════════════
    // Health
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "No authentication required")
    public ResponseEntity<ApiResponse<String>> health() {
        log.info("Health check called on booking-service");
        return ResponseEntity.ok(ApiResponse.success("booking-service is up and running"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // USE CASE 3 — Flight Booking
    // ══════════════════════════════════════════════════════════════════════════

    // ── Step 1: Create Booking ────────────────────────────────────────────────

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary     = "[UC3 Step 1] Create booking",
        description = "Select a flight, enter passenger details. Seats are auto-assigned (1A → 50F). "
                    + "Returns booking with status=PENDING_PAYMENT and nextStep hint for payment. "
                    + "Next → POST /payments/initiate"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Booking created — proceed to payment"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Flight unavailable or not enough seats"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "JWT required")
    })
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String username  = userDetails.getUsername();
        String userEmail = username + "@flight.com";
        log.info("POST /bookings — user={}, flightId={}, passengers={}",
                username, request.getFlightId(), request.getPassengers().size());

        BookingResponse response = bookingService.createBooking(request, username, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Booking created. Proceed to POST /payments/initiate.", response));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // USE CASE 4 — Manage Bookings
    // ══════════════════════════════════════════════════════════════════════════

    // ── Step 1: View booking list ─────────────────────────────────────────────

    @GetMapping("/my")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary     = "[UC4 Step 1] View my bookings",
        description = "Returns all bookings (any status) for the authenticated user, sorted newest first."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Booking list returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "JWT required")
    })
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getMyBookings(
            @AuthenticationPrincipal UserDetails userDetails) {

        String username = userDetails.getUsername();
        log.info("GET /bookings/my — user={}", username);

        List<BookingResponse> list = bookingService.getMyBookings(username);
        return ResponseEntity.ok(ApiResponse.success(list.size() + " booking(s) found", list));
    }

    // ── Upcoming Bookings ─────────────────────────────────────────────────────

    @GetMapping("/my/upcoming")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary     = "View my upcoming bookings",
        description = "Returns CONFIRMED bookings where departure time is in the future, sorted by nearest departure."
    )
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getUpcomingBookings(
            @AuthenticationPrincipal UserDetails userDetails) {

        String username = userDetails.getUsername();
        log.info("GET /bookings/my/upcoming — user={}", username);
        List<BookingResponse> list = bookingService.getUpcomingBookings(username);
        return ResponseEntity.ok(ApiResponse.success(list.size() + " upcoming booking(s)", list));
    }

    // ── Completed Bookings ────────────────────────────────────────────────────

    @GetMapping("/my/completed")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary     = "View my completed bookings",
        description = "Returns CONFIRMED bookings where arrival time is in the past, sorted by most recent."
    )
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getCompletedBookings(
            @AuthenticationPrincipal UserDetails userDetails) {

        String username = userDetails.getUsername();
        log.info("GET /bookings/my/completed — user={}", username);
        List<BookingResponse> list = bookingService.getCompletedBookings(username);
        return ResponseEntity.ok(ApiResponse.success(list.size() + " completed booking(s)", list));
    }

    // ── Step 2 & 3: Select booking + Show details (by ID) ────────────────────

    @GetMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary     = "[UC4 Step 2-3] Get booking details by ID",
        description = "Returns full booking info including passenger list and seat assignments."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Booking found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Booking not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Access denied — not your booking")
    })
    public ResponseEntity<ApiResponse<BookingResponse>> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("GET /bookings/{} — user={}", id, userDetails.getUsername());
        BookingResponse response = bookingService.getBookingById(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Booking found", response));
    }

    // ── Step 2 & 3: Select booking + Show details (by reference) ─────────────

    @GetMapping("/reference/{ref}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary     = "[UC4 Step 2-3] Get booking details by reference",
        description = "Look up booking using the BKG-XXXXXXXX reference number."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Booking found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public ResponseEntity<ApiResponse<BookingResponse>> getByReference(
            @PathVariable String ref,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("GET /bookings/reference/{} — user={}", ref, userDetails.getUsername());
        BookingResponse response = bookingService.getBookingByReference(ref, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Booking found", response));
    }

    // ── Step 3: Show passengers list for a booking ────────────────────────────

    @GetMapping("/{id}/passengers")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary     = "[UC4 Step 3] List passengers for a booking",
        description = "Returns all passengers with their auto-assigned seat numbers."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Passenger list returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Booking not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Access denied")
    })
    public ResponseEntity<ApiResponse<List<PassengerResponse>>> getPassengers(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("GET /bookings/{}/passengers — user={}", id, userDetails.getUsername());
        List<PassengerResponse> passengers = bookingService.getPassengers(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(passengers.size() + " passenger(s)", passengers));
    }

    // ── Step 4a: Modify — update a passenger's details ────────────────────────

    @PatchMapping("/{bookingId}/passengers/{passengerId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary     = "[UC4 Step 4] Modify passenger details",
        description = "Update contact details of a single passenger. "
                    + "Only allowed when booking is in PENDING_PAYMENT status. "
                    + "Seat number cannot be changed (auto-assigned at booking time)."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",  description = "Passenger updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",  description = "Booking not in PENDING_PAYMENT or access denied"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",  description = "Booking or passenger not found")
    })
    public ResponseEntity<ApiResponse<PassengerResponse>> updatePassenger(
            @PathVariable Long bookingId,
            @PathVariable Long passengerId,
            @Valid @RequestBody UpdatePassengerRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("PATCH /bookings/{}/passengers/{} — user={}",
                bookingId, passengerId, userDetails.getUsername());

        PassengerResponse updated = bookingService.updatePassenger(
                bookingId, passengerId, request, userDetails.getUsername());

        return ResponseEntity.ok(ApiResponse.success("Passenger details updated", updated));
    }

    // ── Step 4b: Cancel booking ───────────────────────────────────────────────

    @PostMapping("/{id}/cancel")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary     = "[UC4 Step 4] Cancel a booking",
        description = "Cancels a booking in PENDING_PAYMENT or PAYMENT_FAILED status. "
                    + "CONFIRMED bookings cannot be self-cancelled — contact support for refund."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Booking cancelled"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Cannot cancel (already CONFIRMED or CANCELLED)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public ResponseEntity<ApiResponse<BookingResponse>> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("POST /bookings/{}/cancel — user={}", id, userDetails.getUsername());
        BookingResponse response = bookingService.cancelBooking(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled successfully", response));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Admin Endpoints
    // ══════════════════════════════════════════════════════════════════════════

    // ── Admin: All bookings ───────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary     = "[ADMIN] All bookings",
        description = "Returns all bookings across all users. ADMIN role required."
    )
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getAll() {
        log.info("GET /bookings (admin) — fetching all bookings");
        List<BookingResponse> list = bookingService.getAllBookings();
        return ResponseEntity.ok(ApiResponse.success(list.size() + " booking(s) found", list));
    }

    // ── Admin: Booking statistics ─────────────────────────────────────────────

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary     = "[ADMIN] Booking statistics",
        description = "Returns count breakdown by status and total revenue from CONFIRMED bookings."
    )
    public ResponseEntity<ApiResponse<BookingStatsResponse>> getStats() {
        log.info("GET /bookings/stats (admin) — generating statistics");
        BookingStatsResponse stats = bookingService.getBookingStats();
        return ResponseEntity.ok(ApiResponse.success("Booking statistics", stats));
    }

    // ── Admin: Override booking status ────────────────────────────────────────

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary     = "[ADMIN] Update booking status",
        description = "Manually override the status of any booking. "
                    + "Allowed values: PENDING_PAYMENT, PAYMENT_PROCESSING, CONFIRMED, PAYMENT_FAILED, CANCELLED"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Status updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid status value"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public ResponseEntity<ApiResponse<BookingResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookingStatusRequest request) {

        log.info("PATCH /bookings/{}/status (admin) — newStatus={}, reason={}",
                id, request.getStatus(), request.getReason());

        BookingResponse response = bookingService.updateBookingStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(
                "Booking status updated to " + request.getStatus(), response));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Internal — Cancellation Service Endpoints
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/internal/{bookingId}")
    @Operation(
        summary     = "[INTERNAL] Get booking details — cancellation-service",
        description = "Returns full booking info without ownership check. Requires X-Service-Key header."
    )
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingInternal(
            @PathVariable Long bookingId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Service-Key", required = false) String serviceKey) {

        if (!internalServiceKey.equals(serviceKey)) {
            log.warn("INTERNAL getBooking rejected — invalid X-Service-Key");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Invalid service key"));
        }
        log.info("INTERNAL GET /bookings/internal/{} (cancellation-service)", bookingId);
        BookingResponse response = bookingService.getBookingByIdInternal(bookingId);
        return ResponseEntity.ok(ApiResponse.success("Booking found", response));
    }

    @PatchMapping("/internal/cancel/{bookingId}")
    @Operation(
        summary     = "[INTERNAL] Cancel booking — cancellation-service",
        description = "Sets booking status to CANCELLED without user ownership check. Requires X-Service-Key header."
    )
    public ResponseEntity<ApiResponse<String>> cancelBookingInternal(
            @PathVariable Long bookingId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Service-Key", required = false) String serviceKey) {

        if (!internalServiceKey.equals(serviceKey)) {
            log.warn("INTERNAL cancel rejected — invalid X-Service-Key");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Invalid service key"));
        }
        log.info("INTERNAL PATCH /bookings/internal/cancel/{} (cancellation-service)",
                bookingId);
        bookingService.cancelBookingInternal(bookingId);
        return ResponseEntity.ok(
                ApiResponse.success("Booking " + bookingId + " cancelled by cancellation-service"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Internal — Payment Service Callback
    // ══════════════════════════════════════════════════════════════════════════

    @PatchMapping("/internal/confirm")
    @Operation(
        summary     = "[INTERNAL] Payment service callback",
        description = "Called by payment-service after processing. Requires X-Service-Key header. "
                    + "Updates booking to CONFIRMED or PAYMENT_FAILED and triggers confirmation email."
    )
    public ResponseEntity<ApiResponse<BookingResponse>> confirmBooking(
            @RequestBody ConfirmBookingRequest request,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Service-Key", required = false) String serviceKey) {

        if (!internalServiceKey.equals(serviceKey)) {
            log.warn("INTERNAL confirm rejected — invalid or missing X-Service-Key");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Invalid service key"));
        }
        log.info("INTERNAL /bookings/internal/confirm — ref={}, payRef={}, status={}",
                request.getBookingReference(),
                request.getPaymentReference(),
                request.getStatus());

        BookingResponse response = bookingService.confirmBooking(request);
        return ResponseEntity.ok(
                ApiResponse.success("Booking updated to " + request.getStatus(), response));
    }
}
