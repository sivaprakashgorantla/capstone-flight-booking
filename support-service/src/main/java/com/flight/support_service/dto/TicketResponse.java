package com.flight.support_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Support ticket details")
public class TicketResponse {

    private Long id;

    @Schema(example = "TKT-A1B2C3D4")
    private String ticketReference;

    private String userId;
    private String userEmail;
    private String category;
    private String priority;
    private String subject;
    private String description;
    private String status;

    @Schema(description = "Assigned support agent", example = "Agent Priya Sharma")
    private String assignedTo;

    private String bookingReference;
    private String flightNumber;
    private String resolution;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Schema(description = "Human-readable status message / acknowledgement")
    private String message;
}
