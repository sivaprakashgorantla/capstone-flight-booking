package com.flight.support_service.repository;

import com.flight.support_service.model.SupportTicket;
import com.flight.support_service.model.TicketCategory;
import com.flight.support_service.model.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<SupportTicket, Long> {

    Optional<SupportTicket> findByTicketReference(String ticketReference);

    List<SupportTicket> findByUserIdOrderByCreatedAtDesc(String userId);

    List<SupportTicket> findAllByOrderByCreatedAtDesc();

    List<SupportTicket> findByStatus(TicketStatus status);

    List<SupportTicket> findByCategory(TicketCategory category);

    List<SupportTicket> findByAssignedTo(String assignedTo);

    boolean existsByTicketReference(String ticketReference);

    long countByStatus(TicketStatus status);
}
