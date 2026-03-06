package com.flight.support_service.service;

import com.flight.support_service.model.SupportTicket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationService {

    // ── UC8 Step 4: User receives acknowledgement ─────────────────────────────

    /**
     * Sends ticket creation acknowledgement to the user (UC8 – Step 4).
     */
    public void sendTicketAcknowledgement(SupportTicket ticket) {
        log.info("""
                ╔══════════════════════════════════════════════════════════════╗
                ║              SUPPORT TICKET ACKNOWLEDGEMENT                  ║
                ╠══════════════════════════════════════════════════════════════╣
                ║  To      : {}
                ║  Subject : Your ticket #{} has been received
                ║  ────────────────────────────────────────────────────────── ║
                ║  Dear Customer,                                              ║
                ║                                                              ║
                ║  We have received your support request and assigned it       ║
                ║  ticket reference: {}
                ║                                                              ║
                ║  Category  : {}
                ║  Priority  : {}
                ║  Assigned  : {}
                ║                                                              ║
                ║  Our support team will respond within 24–48 hours.          ║
                ║  Track your ticket at: /support/reference/{}
                ╚══════════════════════════════════════════════════════════════╝
                """,
                ticket.getUserEmail(),
                ticket.getId(),
                ticket.getTicketReference(),
                ticket.getCategory(),
                ticket.getPriority(),
                ticket.getAssignedTo() != null ? ticket.getAssignedTo() : "Pending Assignment",
                ticket.getTicketReference()
        );
    }

    /**
     * Notifies agent of new ticket assignment (UC8 – Step 3).
     */
    public void sendAgentAssignmentNotification(SupportTicket ticket) {
        log.info("""
                ╔══════════════════════════════════════════════════════════════╗
                ║                AGENT ASSIGNMENT NOTIFICATION                 ║
                ╠══════════════════════════════════════════════════════════════╣
                ║  To      : {}@support.flightapp.com
                ║  Subject : New ticket assigned – {}
                ║  ────────────────────────────────────────────────────────── ║
                ║  Ticket Ref : {}
                ║  Category   : {}
                ║  Priority   : {}
                ║  Subject    : {}
                ║  Customer   : {}
                ╚══════════════════════════════════════════════════════════════╝
                """,
                ticket.getAssignedTo(),
                ticket.getTicketReference(),
                ticket.getTicketReference(),
                ticket.getCategory(),
                ticket.getPriority(),
                ticket.getSubject(),
                ticket.getUserEmail()
        );
    }

    /**
     * Notifies user that their ticket has been resolved.
     */
    public void sendResolutionNotification(SupportTicket ticket) {
        log.info("""
                ╔══════════════════════════════════════════════════════════════╗
                ║                  TICKET RESOLVED NOTIFICATION                ║
                ╠══════════════════════════════════════════════════════════════╣
                ║  To      : {}
                ║  Subject : Your ticket {} has been resolved
                ║  ────────────────────────────────────────────────────────── ║
                ║  Resolution: {}
                ║                                                              ║
                ║  Please reply to reopen if you need further assistance.      ║
                ╚══════════════════════════════════════════════════════════════╝
                """,
                ticket.getUserEmail(),
                ticket.getTicketReference(),
                ticket.getResolution()
        );
    }

    /**
     * Notifies user that their ticket status has been updated.
     */
    public void sendStatusUpdateNotification(SupportTicket ticket) {
        log.info("""
                ╔══════════════════════════════════════════════════════════════╗
                ║               TICKET STATUS UPDATE NOTIFICATION              ║
                ╠══════════════════════════════════════════════════════════════╣
                ║  To      : {}
                ║  Ticket  : {}
                ║  Status  : {} → {}
                ╚══════════════════════════════════════════════════════════════╝
                """,
                ticket.getUserEmail(),
                ticket.getTicketReference(),
                "UPDATED",
                ticket.getStatus()
        );
    }
}
