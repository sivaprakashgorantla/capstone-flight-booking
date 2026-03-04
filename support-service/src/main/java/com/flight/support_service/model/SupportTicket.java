package com.flight.support_service.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "support_tickets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_reference", nullable = false, unique = true, length = 15)
    private String ticketReference;              // TKT-XXXXXXXX

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "user_email", nullable = false, length = 150)
    private String userEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TicketCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private TicketPriority priority = TicketPriority.MEDIUM;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TicketStatus status = TicketStatus.OPEN;

    /** Support agent assigned to this ticket. */
    @Column(name = "assigned_to", length = 100)
    private String assignedTo;

    /** Optional: booking reference related to this issue (e.g. BKG-XXXXXXXX). */
    @Column(name = "booking_reference", length = 15)
    private String bookingReference;

    /** Optional: flight number related to this issue (e.g. AI-202). */
    @Column(name = "flight_number", length = 20)
    private String flightNumber;

    /** Resolution message filled by support agent when closing the ticket. */
    @Column(length = 2000)
    private String resolution;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
