package com.auth_service.flight.demo.controller;

import com.auth_service.flight.demo.dto.AuthRequest;
import com.auth_service.flight.demo.dto.AuthResponse;
import com.auth_service.flight.demo.dto.RegisterRequest;
import com.auth_service.flight.demo.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "JWT authentication and authorization endpoints")
public class AuthController {

    private final AuthService authService;

    // ─── Register ──────────────────────────────────────────────────────────────

    @Operation(summary = "Register a new user", description = "Creates a new user account and returns a JWT token")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Username already exists or invalid input",
                    content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("POST /auth/register - username: {}", request.getUsername());
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ─── Login ─────────────────────────────────────────────────────────────────

    @Operation(summary = "Login and get JWT token", description = "Authenticates user credentials and returns a signed JWT token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid username or password",
                    content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        log.info("POST /auth/login - username: {}", request.getUsername());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    // ─── Validate ──────────────────────────────────────────────────────────────

    @Operation(
            summary = "Validate a JWT token",
            description = "Checks if the given token is valid and not expired. Used by the API Gateway for token introspection.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token is valid"),
            @ApiResponse(responseCode = "401", description = "Token is invalid or expired")
    })
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(
            @Parameter(description = "JWT token to validate", required = true)
            @RequestParam("token") String token
    ) {
        log.info("GET /auth/validate - validating token");
        boolean valid = authService.validateToken(token);

        if (valid) {
            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "message", "Token is valid"
            ));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "valid", false,
                "message", "Token is invalid or expired"
        ));
    }

    // ─── Health Check ──────────────────────────────────────────────────────────

    @Operation(summary = "Auth service health check")
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "auth-service"));
    }
}
