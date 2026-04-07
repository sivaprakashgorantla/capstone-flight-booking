package user_service.user_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import user_service.user_service.dto.ApiResponse;
import user_service.user_service.dto.SavedPassengerRequest;
import user_service.user_service.dto.SavedPassengerResponse;
import user_service.user_service.service.SavedPassengerService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users/passengers")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Saved Passengers", description = "Save & reuse passenger profiles so you never re-enter details at booking time")
public class SavedPassengerController {

    private final SavedPassengerService savedPassengerService;

    // ─── List ──────────────────────────────────────────────────────────────────

    @Operation(
        summary     = "List my saved passengers",
        description = "Returns all saved passenger profiles for the authenticated user, ordered by creation date."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Passenger list returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "JWT required")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<SavedPassengerResponse>>> getAll(
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("GET /users/passengers — user: {}", userDetails.getUsername());
        List<SavedPassengerResponse> list = savedPassengerService.getAll(userDetails.getUsername());
        return ResponseEntity.ok(
                ApiResponse.success(list.size() + " saved passenger(s)", list));
    }

    // ─── Get one ───────────────────────────────────────────────────────────────

    @Operation(summary = "Get a saved passenger by ID")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Passenger found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found or not owned by user")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SavedPassengerResponse>> getOne(
            @Parameter(description = "Saved passenger ID") @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("GET /users/passengers/{} — user: {}", id, userDetails.getUsername());
        SavedPassengerResponse response = savedPassengerService.getOne(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Saved passenger found", response));
    }

    // ─── Add ───────────────────────────────────────────────────────────────────

    @Operation(
        summary     = "Add a saved passenger",
        description = "Save a passenger profile with a friendly label (e.g. 'Myself', 'Wife', 'Dad'). "
                    + "Max 10 per account. Labels must be unique per user."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Passenger saved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or duplicate label or limit reached")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<SavedPassengerResponse>> add(
            @Valid @RequestBody SavedPassengerRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("POST /users/passengers — user: {}, label: {}", userDetails.getUsername(), request.getLabel());
        SavedPassengerResponse response = savedPassengerService.add(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Passenger '" + response.getLabel() + "' saved successfully", response));
    }

    // ─── Update ────────────────────────────────────────────────────────────────

    @Operation(
        summary     = "Update a saved passenger",
        description = "Replace all fields of a saved passenger profile. "
                    + "Only the owner can update their own saved passengers."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Passenger updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or duplicate label"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found or not owned by user")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SavedPassengerResponse>> update(
            @Parameter(description = "Saved passenger ID") @PathVariable Long id,
            @Valid @RequestBody SavedPassengerRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("PUT /users/passengers/{} — user: {}", id, userDetails.getUsername());
        SavedPassengerResponse response = savedPassengerService.update(id, userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Passenger '" + response.getLabel() + "' updated", response));
    }

    // ─── Delete ────────────────────────────────────────────────────────────────

    @Operation(
        summary     = "Delete a saved passenger",
        description = "Permanently removes a saved passenger profile. Only the owner can delete."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found or not owned by user")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "Saved passenger ID") @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("DELETE /users/passengers/{} — user: {}", id, userDetails.getUsername());
        savedPassengerService.delete(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Saved passenger deleted"));
    }
}
