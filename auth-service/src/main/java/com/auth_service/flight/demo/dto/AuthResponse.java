package com.auth_service.flight.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "JWT token response")
public class AuthResponse {

    @Schema(description = "JWT Bearer token")
    private String token;

    @Schema(description = "Token validity in milliseconds")
    private long expiresIn;

    @Schema(description = "Authenticated username")
    private String username;

    @Schema(description = "Authenticated email")
    private String email;

    @Schema(description = "Assigned role")
    private String role;
}
