package user_service.user_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload to update user profile")
public class UpdateProfileRequest {

    @Schema(description = "First name")
    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;

    @Schema(description = "Last name")
    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;

    @Schema(description = "Email address", example = "user@example.com")
    @Email(message = "Must be a valid email address")
    private String email;

    @Schema(description = "Phone number", example = "+1234567890")
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phoneNumber;
}
