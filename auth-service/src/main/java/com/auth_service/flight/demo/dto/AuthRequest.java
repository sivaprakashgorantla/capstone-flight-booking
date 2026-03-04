package com.auth_service.flight.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Login credentials")
public class AuthRequest {

    @NotBlank(message = "Username is required")
    @Schema(description = "Username", example = "john_doe")
    private String username;

    @NotBlank(message = "Password is required")
    @Schema(description = "Password", example = "secret123")
    private String password;
}
