package com.flight.notification_service.controller;

import com.flight.notification_service.dto.*;
import com.flight.notification_service.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "UC9 — User notification management and preferences")
public class NotificationController {

    private final NotificationService           notificationService;
    private final NotificationPreferenceService preferenceService;
    private final JwtService                    jwtService;

    // ─────────────────────────────────────────────────────────────────────────
    // Public
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "No auth required")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Notification Service is UP", "UP"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UC9 Step 1: Subscribe to notifications
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/preferences")
    @Operation(
            summary     = "Subscribe to notifications (UC9 Step 1)",
            description = "Creates notification preferences for the authenticated user. " +
                    "Configure which channels (Email/Push/SMS) and types (booking, delay, reminder…) to receive.",
            security    = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Subscribed")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Already subscribed")
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> subscribe(
            @RequestBody(required = false) NotificationPreferenceRequest request,
            HttpServletRequest httpRequest) {

        if (request == null) request = new NotificationPreferenceRequest();  // use all defaults
        String[] info   = extractUserInfo(httpRequest);
        String userId   = info[0];
        String email    = info[1];

        log.info("POST /notifications/preferences  userId={}", userId);
        NotificationPreferenceResponse pref = preferenceService.subscribe(request, userId, email);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(pref.getMessage(), pref));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UC9 Step 3: Manage preferences
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/preferences")
    @Operation(
            summary     = "Get my notification preferences (UC9 Step 3)",
            description = "Returns the current notification preferences for the authenticated user.",
            security    = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> getPreferences(
            HttpServletRequest httpRequest) {

        String userId = extractUserId(httpRequest);
        log.info("GET /notifications/preferences  userId={}", userId);
        return ResponseEntity.ok(ApiResponse.success("Preferences retrieved",
                preferenceService.getPreferences(userId)));
    }

    @PutMapping("/preferences")
    @Operation(
            summary     = "Update notification preferences (UC9 Step 3)",
            description = "Update channel and type preferences. Subscribe first if not already done.",
            security    = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> updatePreferences(
            @Valid @RequestBody NotificationPreferenceRequest request,
            HttpServletRequest httpRequest) {

        String userId = extractUserId(httpRequest);
        log.info("PUT /notifications/preferences  userId={}", userId);
        NotificationPreferenceResponse pref = preferenceService.updatePreferences(request, userId);
        return ResponseEntity.ok(ApiResponse.success(pref.getMessage(), pref));
    }

    @DeleteMapping("/preferences")
    @Operation(
            summary     = "Unsubscribe from all notifications",
            description = "Removes all notification preferences (opt-out).",
            security    = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> unsubscribe(HttpServletRequest httpRequest) {
        String userId = extractUserId(httpRequest);
        log.info("DELETE /notifications/preferences  userId={}", userId);
        preferenceService.unsubscribe(userId);
        return ResponseEntity.ok(ApiResponse.success("Successfully unsubscribed from all notifications"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // User: view notifications
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/my")
    @Operation(
            summary     = "Get my notifications",
            description = "Returns all notifications for the authenticated user, newest first.",
            security    = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyNotifications(
            HttpServletRequest httpRequest) {

        String userId = extractUserId(httpRequest);
        log.info("GET /notifications/my  userId={}", userId);
        List<NotificationResponse> list = notificationService.getMyNotifications(userId);
        return ResponseEntity.ok(ApiResponse.success("Found " + list.size() + " notification(s)", list));
    }

    @GetMapping("/my/unread")
    @Operation(
            summary     = "Get my unread notifications",
            description = "Returns only PENDING or SENT (unread) notifications for the authenticated user.",
            security    = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyUnread(
            HttpServletRequest httpRequest) {

        String userId = extractUserId(httpRequest);
        log.info("GET /notifications/my/unread  userId={}", userId);
        List<NotificationResponse> list = notificationService.getMyUnread(userId);
        return ResponseEntity.ok(ApiResponse.success(
                list.size() + " unread notification(s)", list));
    }

    @GetMapping("/my/unread-count")
    @Operation(
            summary  = "Get unread notification count",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            HttpServletRequest httpRequest) {

        String userId = extractUserId(httpRequest);
        long count = notificationService.countUnread(userId);
        return ResponseEntity.ok(ApiResponse.success("Unread count", Map.of("unread", count)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // User: mark as read
    // ─────────────────────────────────────────────────────────────────────────

    @PatchMapping("/{id}/read")
    @Operation(
            summary     = "Mark notification as read",
            description = "Marks a specific notification as READ. Only the owner can mark their own notifications.",
            security    = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        String userId = extractUserId(httpRequest);
        log.info("PATCH /notifications/{}/read  userId={}", id, userId);
        NotificationResponse n = notificationService.markAsRead(id, userId);
        return ResponseEntity.ok(ApiResponse.success(n.getResponseMessage(), n));
    }

    @PatchMapping("/read-all")
    @Operation(
            summary     = "Mark all notifications as read",
            description = "Marks every unread notification for the authenticated user as READ.",
            security    = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllAsRead(
            HttpServletRequest httpRequest) {

        String userId = extractUserId(httpRequest);
        log.info("PATCH /notifications/read-all  userId={}", userId);
        int count = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success(
                count + " notification(s) marked as read",
                Map.of("markedRead", count)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Admin endpoints
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary  = "Get all notifications (Admin)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getAllNotifications() {
        log.info("GET /notifications/admin/all");
        List<NotificationResponse> list = notificationService.getAllNotifications();
        return ResponseEntity.ok(ApiResponse.success("Total: " + list.size(), list));
    }

    @GetMapping("/admin/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary     = "Filter by status (Admin)",
            description = "Allowed: PENDING | SENT | FAILED | READ",
            security    = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getByStatus(
            @Parameter(example = "SENT") @PathVariable String status) {

        log.info("GET /notifications/admin/status/{}", status);
        List<NotificationResponse> list = notificationService.getByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(list.size() + " result(s)", list));
    }

    @GetMapping("/admin/type/{type}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary     = "Filter by type (Admin)",
            description = "Allowed: BOOKING_CONFIRMATION | DELAY_ALERT | FLIGHT_REMINDER | PAYMENT_SUCCESS | CANCELLATION_CONFIRMATION | GENERAL_ALERT",
            security    = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getByType(
            @Parameter(example = "DELAY_ALERT") @PathVariable String type) {

        log.info("GET /notifications/admin/type/{}", type);
        List<NotificationResponse> list = notificationService.getByType(type);
        return ResponseEntity.ok(ApiResponse.success(list.size() + " result(s)", list));
    }

    @GetMapping("/admin/preferences")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary  = "Get all user preferences (Admin)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<List<NotificationPreferenceResponse>>> getAllPreferences() {
        log.info("GET /notifications/admin/preferences");
        List<NotificationPreferenceResponse> list = preferenceService.getAllPreferences();
        return ResponseEntity.ok(ApiResponse.success(list.size() + " preference record(s)", list));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer "))
            throw new SecurityException("Missing Authorization header");
        return header.substring(7);
    }

    private String extractUserId(HttpServletRequest request) {
        return jwtService.extractUserId(extractToken(request));
    }

    private String[] extractUserInfo(HttpServletRequest request) {
        String token  = extractToken(request);
        String userId = jwtService.extractUserId(token);
        String email  = jwtService.extractUsername(token);   // sub = email in auth-service
        return new String[]{ userId, email };
    }
}
