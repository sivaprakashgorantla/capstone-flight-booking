package com.flight.booking_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update passenger contact details (only allowed for PENDING_PAYMENT bookings)")
public class UpdatePassengerRequest {

    @NotBlank(message = "First name is required")
    @Schema(description = "First name", example = "Rahul")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Schema(description = "Last name", example = "Sharma")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Valid email is required")
    @Schema(description = "Passenger email", example = "rahul.sharma@example.com")
    private String email;

    @NotBlank(message = "Phone is required")
    @Schema(description = "Phone number", example = "+91-9876543210")
    private String phone;

    @NotBlank(message = "Gender is required")
    @Schema(description = "Gender (MALE / FEMALE / OTHER)", example = "MALE")
    private String gender;

    @Schema(description = "Passport number (optional for domestic flights)", example = "P1234567")
    private String passportNumber;
}
