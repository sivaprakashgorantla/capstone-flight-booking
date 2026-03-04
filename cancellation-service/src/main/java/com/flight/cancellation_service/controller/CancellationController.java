package com.flight.cancellation_service.controller;

import com.flight.cancellation_service.dto.*;
import com.flight.cancellation_service.service.CancellationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/cancellations")
@RequiredArgsConstructor
@Tag(
    name        = "Flight Cancellation",
    description = "Use Case 6 — Cancel a confirmed booking, verify refund eligibility, "
                + "process refund, and send confirmation email. "
                + "Refund policy is time-based (hours before departure)."
)
public class CancellationController {

    private final CancellationService cancellationService;

    // ── Health ────────────────────────────────────────────────────────────────

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "No authentication required")
    public ResponseEntity<ApiResponse<String>> health() {
        log.info("Health check called on cancellation-service");
        return ResponseEntity.ok(ApiResponse.success("cancellation-service is up and running"));
    }

    // ── Step 1-5: Initiate Cancellation ──────────────────────────────────────

    @PostMapping("/initiate")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary     = "[UC6 Steps 1-5] Initiate flight cancellation",
        description = """
            Full cancellation workflow in one call:

            **Step 1** — User selects booking (provide bookingId + bookingReference).
            **Step 2** — System verifies eligibility:
              - PENDING_PAYMENT → free cancel (no refund needed).
              - CONFIRMED + flight departed → REJECTED.
              - CONFIRMED + not departed → refund policy applied.
            **Step 3** — Booking cancelled in booking-service.
            **Step 4** — Refund processed (time-based policy):
              - > 48h before departure → **90% refund**
              - 24–48h → **75%**
              - 12–24h → **50%**
              -  6–12h → **25%**
              -  < 6h  → **0%** (no refund)
            **Step 5** — Confirmation email sent (logged to console).
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
            description = "Cancellation processed — check status field for result"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
            description = "Already cancelled, or validation error"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
            description = "JWT required")
    })
    public ResponseEntity<ApiResponse<CancellationResponse>> initiateCancellation(
            @Valid @RequestBody InitiateCancellationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String username = userDetails.getUsername();
        log.info("POST /cancellations/initiate — user={}, bookingId={}, bookingRef={}",
                username, request.getBookingId(), request.getBookingReference());

        CancellationResponse response =
                cancellationService.initiateCancellation(request, username);

        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }

    // ── My Cancellations ──────────────────────────────────────────────────────

    @GetMapping("/my")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary     = "Get my cancellations",
        description = "Returns all cancellation records for the authenticated user, newest first."
    )
    public ResponseEntity<ApiResponse<List<CancellationResponse>>> getMyCancellations(
            @AuthenticationPrincipal UserDetails userDetails) {

        String username = userDetails.getUsername();
        log.info("GET /cancellations/my — user={}", username);

        List<CancellationResponse> list =
                cancellationService.getMyCancellations(username);
        return ResponseEntity.ok(
                ApiResponse.success(list.size() + " cancellation(s) found", list));
    }

    // ── Get by ID ─────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get cancellation by ID")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
            description = "Cancellation found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
            description = "Not found")
    })
    public ResponseEntity<ApiResponse<CancellationResponse>> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("GET /cancellations/{} — user={}", id, userDetails.getUsername());
        CancellationResponse response =
                cancellationService.getById(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Cancellation found", response));
    }

    // ── Get by Reference ──────────────────────────────────────────────────────

    @GetMapping("/reference/{ref}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary     = "Get cancellation by reference",
        description = "Look up using the CAN-XXXXXXXX reference number."
    )
    public ResponseEntity<ApiResponse<CancellationResponse>> getByReference(
            @PathVariable String ref,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("GET /cancellations/reference/{} — user={}", ref, userDetails.getUsername());
        CancellationResponse response =
                cancellationService.getByReference(ref, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Cancellation found", response));
    }

    // ── Get by Booking ID ─────────────────────────────────────────────────────

    @GetMapping("/booking/{bookingId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary     = "Get cancellation by booking ID",
        description = "Retrieve the cancellation record linked to a specific booking."
    )
    public ResponseEntity<ApiResponse<CancellationResponse>> getByBookingId(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("GET /cancellations/booking/{} — user={}", bookingId,
                userDetails.getUsername());
        CancellationResponse response =
                cancellationService.getByBookingId(bookingId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Cancellation found", response));
    }

    // ── Admin: All Cancellations ──────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary     = "[ADMIN] All cancellations",
        description = "Returns all cancellation records across all users. ADMIN role required."
    )
    public ResponseEntity<ApiResponse<List<CancellationResponse>>> getAll() {
        log.info("GET /cancellations (admin) — fetching all records");
        List<CancellationResponse> list = cancellationService.getAllCancellations();
        return ResponseEntity.ok(
                ApiResponse.success(list.size() + " cancellation(s) found", list));
    }
}
