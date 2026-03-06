package com.flight.support_service.service;

import com.flight.support_service.model.SupportTicket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final RestTemplate restTemplate;

    @Value("${email.service.url}")
    private String emailServiceUrl;

    @Value("${internal.service.key}")
    private String internalServiceKey;

    // ── UC8 Step 4: User acknowledgement email ────────────────────────────────

    @Async
    public void sendTicketAcknowledgement(SupportTicket ticket) {
        log.info("""
                [UC8-NOTIFY] TICKET ACKNOWLEDGEMENT
                  To      : {}
                  Ref     : {}   Priority: {}
                  Assigned: {}
                """,
                ticket.getUserEmail(),
                ticket.getTicketReference(),
                ticket.getPriority(),
                ticket.getAssignedTo()
        );
        try {
            Map<String, String> body = Map.of(
                    "toEmail",         ticket.getUserEmail(),
                    "ticketReference", ticket.getTicketReference(),
                    "category",        ticket.getCategory() != null ? ticket.getCategory().name() : "GENERAL_ENQUIRY",
                    "priority",        ticket.getPriority() != null ? ticket.getPriority().name() : "MEDIUM",
                    "assignedTo",      ticket.getAssignedTo() != null ? ticket.getAssignedTo() : "support-team",
                    "subject",         ticket.getSubject()
            );
            restTemplate.postForEntity(
                    emailServiceUrl + "/email/support-ack",
                    new HttpEntity<>(body, buildHeaders()),
                    String.class
            );
            log.info("[UC8-NOTIFY] Acknowledgement email dispatched: ref={}", ticket.getTicketReference());
        } catch (Exception e) {
            log.warn("[UC8-NOTIFY] Could not dispatch acknowledgement email: ref={} error={}",
                    ticket.getTicketReference(), e.getMessage());
        }
    }

    // ── UC8 Step 3: Agent assignment notification ─────────────────────────────

    @Async
    public void sendAgentAssignmentNotification(SupportTicket ticket) {
        log.info("""
                [UC8-NOTIFY] AGENT ASSIGNMENT
                  Agent   : {}@support.skybook.com
                  Ticket  : {}   Priority: {}
                  Category: {}   Customer: {}
                """,
                ticket.getAssignedTo(),
                ticket.getTicketReference(),
                ticket.getPriority(),
                ticket.getCategory(),
                ticket.getUserEmail()
        );
    }

    // ── Resolution notification ───────────────────────────────────────────────

    @Async
    public void sendResolutionNotification(SupportTicket ticket) {
        log.info("[UC8-NOTIFY] TICKET RESOLVED: ref={} to={}", ticket.getTicketReference(), ticket.getUserEmail());
        try {
            Map<String, String> body = Map.of(
                    "toEmail",         ticket.getUserEmail(),
                    "ticketReference", ticket.getTicketReference(),
                    "resolution",      ticket.getResolution() != null ? ticket.getResolution() : ""
            );
            restTemplate.postForEntity(
                    emailServiceUrl + "/email/support-resolved",
                    new HttpEntity<>(body, buildHeaders()),
                    String.class
            );
            log.info("[UC8-NOTIFY] Resolution email dispatched: ref={}", ticket.getTicketReference());
        } catch (Exception e) {
            log.warn("[UC8-NOTIFY] Could not dispatch resolution email: ref={} error={}",
                    ticket.getTicketReference(), e.getMessage());
        }
    }

    // ── Status update notification ────────────────────────────────────────────

    @Async
    public void sendStatusUpdateNotification(SupportTicket ticket) {
        log.info("[UC8-NOTIFY] STATUS UPDATE: ref={} status={} to={}",
                ticket.getTicketReference(), ticket.getStatus(), ticket.getUserEmail());
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Service-Key", internalServiceKey);
        return headers;
    }
}
