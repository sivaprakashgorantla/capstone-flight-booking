package com.flight.support_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Admin request to assign or re-assign a ticket to a support agent")
public class AssignTicketRequest {

    @NotBlank(message = "Agent name is required")
    @Schema(description = "Support agent full name", example = "Agent Vikram Singh")
    private String agentName;
}
