package com.flight.email_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupportAckEmailRequest {

    @Email
    @NotBlank(message = "Recipient email is required")
    private String toEmail;

    @NotBlank(message = "Ticket reference is required")
    private String ticketReference;

    private String category;
    private String priority;
    private String assignedTo;
    private String subject;
}
