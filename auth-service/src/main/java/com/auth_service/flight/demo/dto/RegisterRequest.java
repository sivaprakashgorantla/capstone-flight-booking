package com.auth_service.flight.demo.dto;

import com.auth_service.flight.demo.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "New user registration details")
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Schema(description = "Unique username", example = "john_doe")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    @Schema(description = "Email address", example = "john@example.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    @Schema(description = "Password (min 6 chars)", example = "secret123")
    private String password;

    @Schema(description = "First name", example = "John")
    private String firstName;

    @Schema(description = "Last name", example = "Doe")
    private String lastName;

    @NotNull(message = "Role is required")
    @Schema(description = "User role", example = "USER", allowableValues = {"USER", "ADMIN"})
    private Role role;
}
