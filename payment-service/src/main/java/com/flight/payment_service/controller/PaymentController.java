package com.flight.payment_service.controller;

import com.flight.payment_service.dto.*;
import com.flight.payment_service.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "Payment processing -- initiate, track, and manage payments")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "No auth required")
    public ResponseEntity<ApiResponse<String>> health() {
        log.info("Payment service health check");
        return ResponseEntity.ok(ApiResponse.success("payment-service is up and running"));
    }

    @PostMapping("/initiate")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary     = "Step 2 -- Initiate payment",
        description = "Process payment for a booking. "
                    + "Provide bookingId + bookingReference from Step 1 (POST /bookings). "
                    + "Choose payment method: CREDIT_CARD, DEBIT_CARD, UPI, NET_BANKING. "
                    + "On success: booking is CONFIRMED and confirmation email is logged. "
                    + "Use simulateFailure=true to test payment failure flow."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Payment processed (check status field for SUCCESS/FAILED)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request or duplicate payment"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<PaymentResponse>> initiatePayment(
            @Valid @RequestBody InitiatePaymentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = userDetails.getUsername();
        log.info("POST /payments/initiate -- user={}, bookingRef={}, method={}",
                userId, request.getBookingReference(), request.getPaymentMethod());

        PaymentResponse response = paymentService.initiatePayment(request, userId);

        HttpStatus httpStatus = "SUCCESS".equals(response.getStatus())
                ? HttpStatus.CREATED : HttpStatus.OK;

        return ResponseEntity.status(httpStatus)
                .body(ApiResponse.success(response.getMessage(), response));
    }

    @GetMapping("/my")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get my payments", description = "All payments for the authenticated user")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getMyPayments(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<PaymentResponse> list = paymentService.getMyPayments(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(list.size() + " payment(s) found", list));
    }

    @GetMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get payment by ID")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<ApiResponse<PaymentResponse>> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        PaymentResponse response = paymentService.getPaymentById(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Payment found", response));
    }

    @GetMapping("/reference/{ref}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get payment by reference", description = "e.g. PAY-ABC12345")
    public ResponseEntity<ApiResponse<PaymentResponse>> getByReference(
            @PathVariable String ref,
            @AuthenticationPrincipal UserDetails userDetails) {
        PaymentResponse response = paymentService.getByReference(ref, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Payment found", response));
    }

    @GetMapping("/booking/{bookingId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get payment by booking ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getByBookingId(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal UserDetails userDetails) {
        PaymentResponse response = paymentService.getByBookingId(bookingId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Payment found", response));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "All payments (ADMIN)", description = "All payments across all users -- ADMIN only")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getAll() {
        List<PaymentResponse> list = paymentService.getAllPayments();
        return ResponseEntity.ok(ApiResponse.success(list.size() + " payment(s) found", list));
    }
}
