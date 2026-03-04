package com.flight.support_service.service;

import com.flight.support_service.dto.*;
import com.flight.support_service.exception.BadRequestException;
import com.flight.support_service.exception.TicketNotFoundException;
import com.flight.support_service.model.*;
import com.flight.support_service.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupportService {

    private final TicketRepository ticketRepository;
    private final NotificationService notificationService;

    // ── Agent roster keyed by category ───────────────────────────────────────

    private static final Map<TicketCategory, String> AGENT_MAP = Map.of(
            TicketCategory.BOOKING_ISSUE,       "agent-booking",
            TicketCategory.PAYMENT_ISSUE,        "agent-payments",
            TicketCategory.CANCELLATION_ISSUE,   "agent-cancellations",
            TicketCategory.REFUND_ISSUE,         "agent-refunds",
            TicketCategory.FLIGHT_DELAY,         "agent-operations",
            TicketCategory.BAGGAGE,              "agent-baggage",
            TicketCategory.GENERAL_ENQUIRY,      "agent-general",
            TicketCategory.TECHNICAL_ISSUE,      "agent-tech"
    );

    // ── Priority matrix by category ───────────────────────────────────────────

    private static final Map<TicketCategory, TicketPriority> PRIORITY_MAP = Map.of(
            TicketCategory.PAYMENT_ISSUE,        TicketPriority.HIGH,
            TicketCategory.CANCELLATION_ISSUE,   TicketPriority.HIGH,
            TicketCategory.REFUND_ISSUE,         TicketPriority.HIGH,
            TicketCategory.TECHNICAL_ISSUE,      TicketPriority.URGENT,
            TicketCategory.BOOKING_ISSUE,        TicketPriority.MEDIUM,
            TicketCategory.FLIGHT_DELAY,         TicketPriority.MEDIUM,
            TicketCategory.BAGGAGE,              TicketPriority.LOW,
            TicketCategory.GENERAL_ENQUIRY,      TicketPriority.LOW
    );

    // ── UC8 Step 1–4: Submit ticket ───────────────────────────────────────────

    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request,
                                       String userId,
                                       String userEmail) {

        log.info("Creating support ticket for user={} category={}", userId, request.getCategory());

        // Auto-determine priority and agent
        TicketPriority priority = PRIORITY_MAP.getOrDefault(
                request.getCategory(), TicketPriority.MEDIUM);
        String assignedTo = AGENT_MAP.getOrDefault(
                request.getCategory(), "agent-general");

        // UC8 Step 2: Build ticket entity
        SupportTicket ticket = SupportTicket.builder()
                .ticketReference(generateReference())
                .userId(userId)
                .userEmail(userEmail)
                .category(request.getCategory())
                .priority(priority)
                .subject(request.getSubject())
                .description(request.getDescription())
                .bookingReference(request.getBookingReference())
                .flightNumber(request.getFlightNumber())
                .status(TicketStatus.OPEN)
                .assignedTo(assignedTo)   // UC8 Step 3: auto-assign
                .build();

        SupportTicket saved = ticketRepository.save(ticket);

        log.info("Ticket created: ref={} priority={} assignedTo={}",
                saved.getTicketReference(), saved.getPriority(), saved.getAssignedTo());

        // UC8 Step 3: notify agent
        notificationService.sendAgentAssignmentNotification(saved);

        // UC8 Step 4: acknowledge user
        notificationService.sendTicketAcknowledgement(saved);

        return toResponse(saved,
                "Ticket submitted successfully. Ticket reference: " + saved.getTicketReference());
    }

    // ── User: view own tickets ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TicketResponse> getMyTickets(String userId) {
        log.info("Fetching tickets for userId={}", userId);
        return ticketRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(t -> toResponse(t, null))
                .toList();
    }

    // ── Admin: get any ticket by ID (no ownership check) ─────────────────────

    @Transactional(readOnly = true)
    public TicketResponse getTicketByIdAdmin(Long id) {
        return toResponse(findById(id), null);
    }

    // ── User: get own ticket by ID ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TicketResponse getMyTicketById(Long id, String userId) {
        SupportTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
        if (!ticket.getUserId().equals(userId)) {
            throw new TicketNotFoundException(id);
        }
        return toResponse(ticket, null);
    }

    // ── User / Admin: get by reference ───────────────────────────────────────

    @Transactional(readOnly = true)
    public TicketResponse getByReference(String ref, String userId, boolean isAdmin) {
        SupportTicket ticket = ticketRepository.findByTicketReference(ref)
                .orElseThrow(() -> new TicketNotFoundException("No ticket found with reference: " + ref));
        if (!isAdmin && !ticket.getUserId().equals(userId)) {
            throw new TicketNotFoundException("No ticket found with reference: " + ref);
        }
        return toResponse(ticket, null);
    }

    // ── Admin: get all tickets ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TicketResponse> getAllTickets() {
        log.info("Admin: fetching all support tickets");
        return ticketRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(t -> toResponse(t, null))
                .toList();
    }

    // ── Admin: get by status ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TicketResponse> getByStatus(String statusStr) {
        TicketStatus status = parseStatus(statusStr);
        return ticketRepository.findByStatus(status)
                .stream()
                .map(t -> toResponse(t, null))
                .toList();
    }

    // ── Admin: get by category ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TicketResponse> getByCategory(String categoryStr) {
        TicketCategory category = parseCategory(categoryStr);
        return ticketRepository.findByCategory(category)
                .stream()
                .map(t -> toResponse(t, null))
                .toList();
    }

    // ── Admin: assign ticket ──────────────────────────────────────────────────

    @Transactional
    public TicketResponse assignTicket(Long id, AssignTicketRequest request) {
        SupportTicket ticket = findById(id);

        if (ticket.getStatus() == TicketStatus.RESOLVED ||
                ticket.getStatus() == TicketStatus.CLOSED) {
            throw new BadRequestException("Cannot assign a " + ticket.getStatus() + " ticket");
        }

        ticket.setAssignedTo(request.getAgentName());
        if (ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }

        SupportTicket saved = ticketRepository.save(ticket);
        log.info("Ticket {} assigned to agent: {}", id, request.getAgentName());
        return toResponse(saved, "Ticket assigned to " + request.getAgentName());
    }

    // ── Admin: update status ──────────────────────────────────────────────────

    @Transactional
    public TicketResponse updateStatus(Long id, UpdateTicketStatusRequest request) {
        SupportTicket ticket = findById(id);
        TicketStatus newStatus = parseStatus(request.getStatus());

        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new BadRequestException("Cannot update a CLOSED ticket");
        }

        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(newStatus);
        SupportTicket saved = ticketRepository.save(ticket);

        log.info("Ticket {} status updated: {} → {}", id, oldStatus, newStatus);
        notificationService.sendStatusUpdateNotification(saved);

        return toResponse(saved, "Status updated from " + oldStatus + " to " + newStatus);
    }

    // ── Admin: resolve ticket ─────────────────────────────────────────────────

    @Transactional
    public TicketResponse resolveTicket(Long id, ResolveTicketRequest request) {
        SupportTicket ticket = findById(id);

        if (ticket.getStatus() == TicketStatus.RESOLVED ||
                ticket.getStatus() == TicketStatus.CLOSED) {
            throw new BadRequestException("Ticket is already " + ticket.getStatus());
        }

        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolution(request.getResolution());
        ticket.setResolvedAt(LocalDateTime.now());

        SupportTicket saved = ticketRepository.save(ticket);
        log.info("Ticket {} resolved by admin", id);

        notificationService.sendResolutionNotification(saved);
        return toResponse(saved, "Ticket resolved successfully");
    }

    // ── Admin: close ticket ───────────────────────────────────────────────────

    @Transactional
    public TicketResponse closeTicket(Long id) {
        SupportTicket ticket = findById(id);

        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new BadRequestException("Ticket is already CLOSED");
        }

        ticket.setStatus(TicketStatus.CLOSED);
        SupportTicket saved = ticketRepository.save(ticket);
        log.info("Ticket {} closed", id);
        return toResponse(saved, "Ticket closed successfully");
    }

    // ── Admin: stats ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Long> getStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("total",        ticketRepository.count());
        stats.put("open",         ticketRepository.countByStatus(TicketStatus.OPEN));
        stats.put("inProgress",   ticketRepository.countByStatus(TicketStatus.IN_PROGRESS));
        stats.put("awaitingUser", ticketRepository.countByStatus(TicketStatus.AWAITING_USER));
        stats.put("resolved",     ticketRepository.countByStatus(TicketStatus.RESOLVED));
        stats.put("closed",       ticketRepository.countByStatus(TicketStatus.CLOSED));
        return stats;
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private SupportTicket findById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
    }

    private TicketStatus parseStatus(String value) {
        try {
            return TicketStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + value +
                    ". Allowed: OPEN, IN_PROGRESS, AWAITING_USER, RESOLVED, CLOSED");
        }
    }

    private TicketCategory parseCategory(String value) {
        try {
            return TicketCategory.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid category: " + value);
        }
    }

    private String generateReference() {
        String ref = "TKT-" + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 8).toUpperCase();
        // Guard against (astronomically unlikely) collision
        return ticketRepository.existsByTicketReference(ref)
                ? generateReference()
                : ref;
    }

    private TicketResponse toResponse(SupportTicket t, String message) {
        return TicketResponse.builder()
                .id(t.getId())
                .ticketReference(t.getTicketReference())
                .userId(t.getUserId())
                .userEmail(t.getUserEmail())
                .category(t.getCategory() != null ? t.getCategory().name() : null)
                .priority(t.getPriority() != null ? t.getPriority().name() : null)
                .status(t.getStatus() != null ? t.getStatus().name() : null)
                .subject(t.getSubject())
                .description(t.getDescription())
                .bookingReference(t.getBookingReference())
                .flightNumber(t.getFlightNumber())
                .assignedTo(t.getAssignedTo())
                .resolution(t.getResolution())
                .resolvedAt(t.getResolvedAt())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .message(message)
                .build();
    }
}
