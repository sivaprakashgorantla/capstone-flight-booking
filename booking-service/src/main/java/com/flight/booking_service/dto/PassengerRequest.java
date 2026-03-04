package com.flight.booking_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Passenger details for booking")
public class PassengerRequest {

    @NotBlank(message = "First name is required")
    @Schema(description = "First name", example = "Rahul")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Schema(description = "Last name", example = "Sharma")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Valid email is required")
    @Schema(description = "Passenger email", example = "rahul@example.com")
    private String email;

    @NotBlank(message = "Phone is required")
    @Schema(description = "Passenger phone", example = "+91-9876543210")
    private String phone;

    @Min(value = 1, message = "Age must be at least 1")
    @Max(value = 120, message = "Age must be less than 120")
    @Schema(description = "Passenger age", example = "30")
    private int age;

    @NotBlank(message = "Gender is required")
    @Schema(description = "Gender (MALE/FEMALE/OTHER)", example = "MALE")
    private String gender;

    @Schema(description = "Passport number (optional for domestic)", example = "P1234567")
    private String passportNumber;
}
