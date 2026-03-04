package com.flight.support_service.config;

import com.flight.support_service.model.*;
import com.flight.support_service.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Seeds sample support tickets on startup (idempotent — checks by reference).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final TicketRepository ticketRepository;

    private static final List<String> SEED_REFS = List.of(
            "TKT-SEED0001",
            "TKT-SEED0002",
            "TKT-SEED0003",
            "TKT-SEED0004",
            "TKT-SEED0005"
    );

    @Override
    public void run(String... args) {
        long existing = SEED_REFS.stream()
                .filter(ticketRepository::existsByTicketReference)
                .count();

        if (existing == SEED_REFS.size()) {
            log.info("[DataSeeder] Seed data already present — skipping.");
            return;
        }

        log.info("[DataSeeder] Seeding {} sample support tickets...", SEED_REFS.size());

        // ── Ticket 1: Booking issue – resolved ────────────────────────────────
        if (!ticketRepository.existsByTicketReference("TKT-SEED0001")) {
            ticketRepository.save(SupportTicket.builder()
                    .ticketReference("TKT-SEED0001")
                    .userId("user-001")
                    .userEmail("alice@example.com")
                    .category(TicketCategory.BOOKING_ISSUE)
                    .priority(TicketPriority.MEDIUM)
                    .subject("Unable to complete booking — seat not assigned")
                    .description("I tried to book Flight AI-202 from Delhi to Mumbai on 15-Mar but " +
                            "the system did not assign me a seat and the booking status is stuck at PENDING_PAYMENT.")
                    .bookingReference("BKG-A1B2C3D4")
                    .flightNumber("AI-202")
                    .status(TicketStatus.RESOLVED)
                    .assignedTo("agent-booking")
                    .resolution("Seat 14B has been manually assigned and booking confirmed. " +
                            "Payment link resent to your registered email.")
                    .resolvedAt(LocalDateTime.now().minusDays(1))
                    .build());
        }

        // ── Ticket 2: Payment issue – in progress ─────────────────────────────
        if (!ticketRepository.existsByTicketReference("TKT-SEED0002")) {
            ticketRepository.save(SupportTicket.builder()
                    .ticketReference("TKT-SEED0002")
                    .userId("user-002")
                    .userEmail("bob@example.com")
                    .category(TicketCategory.PAYMENT_ISSUE)
                    .priority(TicketPriority.HIGH)
                    .subject("Payment deducted but booking not confirmed")
                    .description("₹12,500 was deducted from my account on 28-Feb but my booking " +
                            "BKG-XY12 still shows PAYMENT_PROCESSING status. Transaction ID: TXN-9987654.")
                    .bookingReference("BKG-XY120000")
                    .status(TicketStatus.IN_PROGRESS)
                    .assignedTo("agent-payments")
                    .build());
        }

        // ── Ticket 3: Refund issue – awaiting user ────────────────────────────
        if (!ticketRepository.existsByTicketReference("TKT-SEED0003")) {
            ticketRepository.save(SupportTicket.builder()
                    .ticketReference("TKT-SEED0003")
                    .userId("user-003")
                    .userEmail("carol@example.com")
                    .category(TicketCategory.REFUND_ISSUE)
                    .priority(TicketPriority.HIGH)
                    .subject("Refund not received after cancellation")
                    .description("I cancelled booking CAN-AB12CD34 on 20-Feb. The cancellation " +
                            "confirmation showed a 75% refund, but I haven't received the amount yet.")
                    .bookingReference("BKG-REFUND01")
                    .status(TicketStatus.AWAITING_USER)
                    .assignedTo("agent-refunds")
                    .build());
        }

        // ── Ticket 4: Flight delay – open ─────────────────────────────────────
        if (!ticketRepository.existsByTicketReference("TKT-SEED0004")) {
            ticketRepository.save(SupportTicket.builder()
                    .ticketReference("TKT-SEED0004")
                    .userId("user-004")
                    .userEmail("dave@example.com")
                    .category(TicketCategory.FLIGHT_DELAY)
                    .priority(TicketPriority.MEDIUM)
                    .subject("Flight 6E-301 delayed by 5 hours — requesting compensation")
                    .description("My flight 6E-301 from Bangalore to Hyderabad on 01-Mar was delayed " +
                            "by over 5 hours. I missed a connecting flight. Requesting meal vouchers and " +
                            "compensation as per DGCA guidelines.")
                    .flightNumber("6E-301")
                    .status(TicketStatus.OPEN)
                    .assignedTo("agent-operations")
                    .build());
        }

        // ── Ticket 5: Technical issue – urgent ───────────────────────────────
        if (!ticketRepository.existsByTicketReference("TKT-SEED0005")) {
            ticketRepository.save(SupportTicket.builder()
                    .ticketReference("TKT-SEED0005")
                    .userId("user-005")
                    .userEmail("eve@example.com")
                    .category(TicketCategory.TECHNICAL_ISSUE)
                    .priority(TicketPriority.URGENT)
                    .subject("App crashes when accessing My Bookings page")
                    .description("The mobile application crashes every time I navigate to the " +
                            "'My Bookings' screen. App version: 3.4.1, Device: Android 13. " +
                            "This has been happening since the last update on 01-Mar.")
                    .status(TicketStatus.OPEN)
                    .assignedTo("agent-tech")
                    .build());
        }

        log.info("[DataSeeder] ✓ Sample support tickets seeded successfully.");
    }
}
