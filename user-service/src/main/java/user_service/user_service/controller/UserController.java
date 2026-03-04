package user_service.user_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import user_service.user_service.dto.*;
import user_service.user_service.service.UserService;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "User Management", description = "Profile, password, role and account management endpoints")
public class UserController {

    private final UserService userService;

    // ─── Health ───────────────────────────────────────────────────────────────

    @Operation(summary = "Health check", description = "Returns service status (public)")
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "user-service"));
    }

    // ─── My Profile ───────────────────────────────────────────────────────────

    @Operation(summary = "Get my profile", description = "Returns the authenticated user's profile")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile fetched",
                    content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("GET /users/profile — user: {}", userDetails.getUsername());
        UserProfileResponse profile = userService.getMyProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Profile fetched successfully", profile));
    }

    @Operation(summary = "Update my profile", description = "Updates firstName, lastName, email or phoneNumber")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or email already in use")
    })
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {

        log.info("PUT /users/profile — user: {}", userDetails.getUsername());
        UserProfileResponse updated = userService.updateProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updated));
    }

    // ─── Password ─────────────────────────────────────────────────────────────

    @Operation(summary = "Change password", description = "Verifies current password then sets the new one")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password changed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Wrong current password or passwords don't match")
    })
    @PatchMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {

        log.info("PATCH /users/change-password — user: {}", userDetails.getUsername());
        userService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    // ─── Account ──────────────────────────────────────────────────────────────

    @Operation(summary = "Deactivate my account", description = "Soft-deactivates the authenticated user's account")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Account deactivated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Account already deactivated")
    })
    @PatchMapping("/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateAccount(
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("PATCH /users/deactivate — user: {}", userDetails.getUsername());
        userService.deactivateAccount(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Account deactivated successfully"));
    }

    // ─── Admin — List & Get ───────────────────────────────────────────────────

    @Operation(summary = "[ADMIN] Get all users", description = "Returns all registered users ordered by creation date")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Users list returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserProfileResponse>>> getAllUsers() {
        log.info("GET /users — admin listing all users");
        List<UserProfileResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success("Users fetched successfully", users));
    }

    @Operation(summary = "[ADMIN] Get user by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserById(
            @Parameter(description = "User ID", required = true) @PathVariable Long id) {

        log.info("GET /users/{} — admin fetching user", id);
        UserProfileResponse user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("User fetched successfully", user));
    }

    // ─── Admin — Role Management ──────────────────────────────────────────────

    @Operation(summary = "[ADMIN] Update user role", description = "Assigns USER or ADMIN role to a specific user")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Role updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required")
    })
    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateRole(
            @Parameter(description = "User ID", required = true) @PathVariable Long id,
            @Valid @RequestBody RoleUpdateRequest request) {

        log.info("PATCH /users/{}/role — new role: {}", id, request.getRole());
        UserProfileResponse updated = userService.updateUserRole(id, request);
        return ResponseEntity.ok(ApiResponse.success("Role updated to " + request.getRole(), updated));
    }

    // ─── Admin — Account Activation ───────────────────────────────────────────

    @Operation(summary = "[ADMIN] Activate a deactivated user account")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Account activated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Account already active"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> activateUser(
            @Parameter(description = "User ID", required = true) @PathVariable Long id) {

        log.info("PATCH /users/{}/activate — admin activating user", id);
        userService.activateUser(id);
        return ResponseEntity.ok(ApiResponse.success("Account activated successfully"));
    }
}
