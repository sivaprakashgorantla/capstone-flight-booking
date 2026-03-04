package user_service.user_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User profile details")
public class UserProfileResponse {

    @Schema(description = "User ID")
    private Long id;

    @Schema(description = "Unique username")
    private String username;

    @Schema(description = "Email address")
    private String email;

    @Schema(description = "First name")
    private String firstName;

    @Schema(description = "Last name")
    private String lastName;

    @Schema(description = "Phone number")
    private String phoneNumber;

    @Schema(description = "Assigned role", example = "USER")
    private String role;

    @Schema(description = "Account active status")
    private boolean active;

    @Schema(description = "Account creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;
}
