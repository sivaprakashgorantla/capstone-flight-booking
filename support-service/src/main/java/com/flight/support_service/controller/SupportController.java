package com.flight.support_service.controller;

import com.flight.support_service.dto.*;
import com.flight.support_service.service.JwtService;
import com.flight.support_service.service.SupportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/support")
@RequiredArgsConstructor
@Tag(name = "Support Tickets", description = "Customer support ticket management (UC8)")
public class SupportController {

    private final SupportService supportService;
    private final JwtService jwtService;

    // ─────────────────────────────────────────────────────────────────────────
    // Public
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Returns service status (no auth required)")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Support Service is running", "UP"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // User endpoints
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * UC8 Step 1–4: Submit a support ticket.
     */
    @PostMapping
    @Operation(
            summary     = "Submit support ticket",
            description = "UC8: User submits a support request. System creates the ticket, " +
                    "auto-assigns it to the appropriate agent, and sends an acknowledgement email.",
            security    = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Ticket created")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<ApiResponse<TicketResponse>> createTicket(
            @Valid @RequestBody CreateTicketRequest request,
            HttpServletRequest httpRequest) {

        String[] userInfo = extractUserInfo(httpRequest);
        String userId    = userInfo[0];
        String userEmail = userInfo[1];

        log.info("POST /support  userId={} category={}", userId, request.getCategory());

        TicketResponse ticket = supportService.createTicket(request, userId, userEmail);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(ticket.getMessage(), ticket));
    }

    /**
     * UC8: User views their own tickets.
     */
    @GetMapping("/my")
    @Operation(
            summary     = "Get my tickets",
            description = "Returns all support tickets submitted by the authenticated user, newest first.",
            security    = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<List<TicketResponse>>> getMyTickets(
            HttpServletRequest httpRequest) {

        String userId = extractUserId(httpRequest);
        log.info("GET /support/my  userId={}", userId);

        List<TicketResponse> tickets = supportService.getMyTickets(userId);
        return ResponseEntity.ok(ApiResponse.success(
                "Found " + tickets.size() + " ticket(s)", tickets));
    }

    /**
     * UC8: User views a specific ticket by ID.
     */
    @GetMapping("/my/{id}")
    @Operation(
            summary     = "Get my ticket by ID",
            description = "Returns ticket details. Only the ticket owner can access it.",
            security    = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<TicketResponse>> getMyTicketById(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        String userId = extractUserId(httpRequest);
        log.info("GET /support/my/{}  userId={}", id, userId);

        TicketResponse ticket = supportService.getMyTicketById(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Ticket retrieved", ticket));
    }

    /**
     * User/Admin: look up ticket by reference (e.g. TKT-ABCD1234).
     */
    @GetMapping("/reference/{ref}")
    @Operation(
            summary     = "Get ticket by reference",
            description = "Look up a ticket by its TKT-XXXXXXXX reference. " +
                    "Users can only view their own tickets; admins can view all.",
            security    = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<TicketResponse>> getByReference(
            @PathVariable String ref,
            HttpServletRequest httpRequest) {

        String token   = extractToken(httpRequest);
        String userId  = jwtService.extractUserId(token);
        String role    = jwtService.extractRole(token);
        boolean isAdmin = role.contains("ADMIN");

        log.info("GET /support/reference/{}  userId={} isAdmin={}", ref, userId, isAdmin);

        TicketResponse ticket = supportService.getByReference(ref, userId, isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Ticket retrieved", ticket));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Admin endpoints
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary     = "Get all tickets (Admin)",
            description = "Returns every support ticket in the system, newest first.",
            security    = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<List<TicketResponse>>> getAllTickets() {
        log.info("GET /support/admin/all");
        List<TicketResponse> tickets = supportService.getAllTickets();
        return ResponseEntity.ok(ApiResponse.success(
                "Total tickets: " + tickets.size(), tickets));
    }

    @GetMapping("/admin/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary     = "Get tickets by status (Admin)",
            description = "Filter tickets by status: OPEN | IN_PROGRESS | AWAITING_USER | RESOLVED | CLOSED",
            security    = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<List<TicketResponse>>> getByStatus(
            @Parameter(description = "Ticket status", example = "OPEN")
            @PathVariable String status) {

        log.info("GET /support/admin/status/{}", status);
        List<TicketResponse> tickets = supportService.getByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(
                tickets.size() + " ticket(s) with status " + status.toUpperCase(), tickets));
    }

    @GetMapping("/admin/category/{category}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary     = "Get tickets by category (Admin)",
            description = "Filter tickets by category, e.g. BOOKING_ISSUE, PAYMENT_ISSUE, REFUND_ISSUE …",
            security    = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<List<TicketResponse>>> getByCategory(
            @Parameter(description = "Ticket category", example = "PAYMENT_ISSUE")
            @PathVariable String category) {

        log.info("GET /support/admin/category/{}", category);
        List<TicketResponse> tickets = supportService.getByCategory(category);
        return ResponseEntity.ok(ApiResponse.success(
                tickets.size() + " ticket(s) in category " + category.toUpperCase(), tickets));
    }

    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary     = "Get any ticket by ID (Admin)",
            description = "Admin can retrieve any ticket regardless of owner.",
            security    = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<TicketResponse>> getTicketById(
            @PathVariable Long id) {

        log.info("GET /support/admin/{}", id);
        TicketResponse ticket = supportService.getTicketByIdAdmin(id);
        return ResponseEntity.ok(ApiResponse.success("Ticket retrieved", ticket));
    }

    @PostMapping("/admin/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary     = "Assign ticket to agent (Admin)",
            description = "UC8 Step 3: manually (re)assign a ticket to a support agent.",
            security    = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<TicketResponse>> assignTicket(
            @PathVariable Long id,
            @Valid @RequestBody AssignTicketRequest request) {

        log.info("POST /support/admin/{}/assign  agent={}", id, request.getAgentName());
        TicketResponse ticket = supportService.assignTicket(id, request);
        return ResponseEntity.ok(ApiResponse.success(ticket.getMessage(), ticket));
    }

    @PatchMapping("/admin/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary     = "Update ticket status (Admin)",
            description = "Move ticket through the status lifecycle: OPEN → IN_PROGRESS → AWAITING_USER → RESOLVED → CLOSED",
            security    = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<TicketResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketStatusRequest request) {

        log.info("PATCH /support/admin/{}/status  newStatus={}", id, request.getStatus());
        TicketResponse ticket = supportService.updateStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(ticket.getMessage(), ticket));
    }

    @PatchMapping("/admin/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary     = "Resolve ticket (Admin)",
            description = "Mark ticket as RESOLVED and provide resolution details. Sends notification email to user.",
            security    = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<TicketResponse>> resolveTicket(
            @PathVariable Long id,
            @Valid @RequestBody ResolveTicketRequest request) {

        log.info("PATCH /support/admin/{}/resolve", id);
        TicketResponse ticket = supportService.resolveTicket(id, request);
        return ResponseEntity.ok(ApiResponse.success(ticket.getMessage(), ticket));
    }

    @PatchMapping("/admin/{id}/close")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary     = "Close ticket (Admin)",
            description = "Permanently close a support ticket (RESOLVED or CLOSED state).",
            security    = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<TicketResponse>> closeTicket(@PathVariable Long id) {
        log.info("PATCH /support/admin/{}/close", id);
        TicketResponse ticket = supportService.closeTicket(id);
        return ResponseEntity.ok(ApiResponse.success(ticket.getMessage(), ticket));
    }

    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary     = "Ticket statistics (Admin)",
            description = "Returns ticket counts grouped by status.",
            security    = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Map<String, Long>>> getStats() {
        log.info("GET /support/admin/stats");
        Map<String, Long> stats = supportService.getStats();
        return ResponseEntity.ok(ApiResponse.success("Support ticket statistics", stats));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new SecurityException("Missing Authorization header");
        }
        return header.substring(7);
    }

    private String extractUserId(HttpServletRequest request) {
        return jwtService.extractUserId(extractToken(request));
    }

    private String extractUsername(HttpServletRequest request) {
        return jwtService.extractUsername(extractToken(request));
    }

    /**
     * Returns [userId, userEmail].
     * userId    → from "userId" claim (auth-service sets this since the JWT fix)
     * userEmail → from "email"  claim (auth-service sets this since the JWT fix)
     * Both fall back gracefully to the sub (username) if the claim is absent.
     */
    private String[] extractUserInfo(HttpServletRequest request) {
        String token     = extractToken(request);
        String userId    = jwtService.extractUserId(token);
        String userEmail = jwtService.extractEmail(token);   // "email" claim, not sub
        return new String[]{ userId, userEmail };
    }
}
