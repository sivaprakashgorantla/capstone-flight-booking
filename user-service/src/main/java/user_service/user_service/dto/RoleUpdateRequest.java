package user_service.user_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import user_service.user_service.model.Role;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload to update a user's role (Admin only)")
public class RoleUpdateRequest {

    @NotNull(message = "Role is required")
    @Schema(description = "New role to assign", example = "ADMIN", allowableValues = {"USER", "ADMIN"})
    private Role role;
}
