package com.flight.email_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WelcomeEmailRequest {

    @NotBlank(message = "Recipient email is required")
    @Email(message = "Valid email address required")
    private String toEmail;

    @NotBlank(message = "Username is required")
    private String username;
}
