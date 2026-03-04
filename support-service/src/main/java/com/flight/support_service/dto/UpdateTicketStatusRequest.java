package com.flight.support_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Admin request to update a ticket's status")
public class UpdateTicketStatusRequest {

    @NotBlank(message = "Status is required")
    @Schema(
        description = "New ticket status",
        example = "IN_PROGRESS",
        allowableValues = {
            "OPEN", "IN_PROGRESS", "AWAITING_USER", "RESOLVED", "CLOSED"
        }
    )
    private String status;
}
