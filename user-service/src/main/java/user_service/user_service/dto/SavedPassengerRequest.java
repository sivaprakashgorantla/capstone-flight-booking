package user_service.user_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "Request to save a passenger profile for reuse in future bookings")
public class SavedPassengerRequest {

    @NotBlank(message = "Label is required (e.g. Myself, Wife, Dad)")
    @Size(max = 50, message = "Label must be 50 characters or less")
    @Schema(description = "Friendly nickname for this passenger", example = "Myself")
    private String label;

    @NotBlank(message = "First name is required")
    @Size(max = 50)
    @Schema(example = "Ravi")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50)
    @Schema(example = "Kumar")
    private String lastName;

    @Min(value = 1, message = "Age must be at least 1")
    @Max(value = 120, message = "Age must be 120 or less")
    @Schema(example = "28")
    private int age;

    @NotBlank(message = "Gender is required")
    @Pattern(regexp = "MALE|FEMALE|OTHER", message = "Gender must be MALE, FEMALE or OTHER")
    @Schema(example = "MALE")
    private String gender;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    @Schema(example = "ravi@example.com")
    private String email;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "\\d{10}", message = "Phone must be exactly 10 digits")
    @Schema(example = "9876543210")
    private String phone;

    @Schema(description = "Optional — required for international flights", example = "P1234567")
    private String passportNumber;
}
