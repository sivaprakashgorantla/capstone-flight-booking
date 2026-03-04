package com.auth_service.flight.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Token and new password for password reset")
public class ResetPasswordRequest {

    @NotBlank(message = "Reset token is required")
    @Schema(description = "UUID token received in the reset email")
    private String token;

    @NotBlank(message = "New password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    @Schema(description = "New password (min 6 chars)")
    private String newPassword;

    @NotBlank(message = "Confirm password is required")
    @Schema(description = "Must match newPassword")
    private String confirmPassword;
}
