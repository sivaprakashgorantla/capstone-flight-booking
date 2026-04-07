package com.flight.ai.controller;

import com.flight.ai.dto.ApiResponse;
import com.flight.ai.dto.ChatRequest;
import com.flight.ai.dto.ChatResponse;
import com.flight.ai.service.ChatbotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Tag(name = "AI Chatbot", description = "AI-powered assistant for flight booking queries")
public class AiController {

    private final ChatbotService chatbotService;

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "No authentication required")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("ai-service is up and running", null));
    }

    @PostMapping("/chat")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Chat with AI assistant",
        description = """
            Send a message to the AI booking assistant. It understands:
            - "Show my upcoming bookings" → lists future confirmed flights
            - "Show my completed bookings" → lists past confirmed flights
            - "Show all my bookings" → full booking history
            - "Help me book a ticket" → guides through booking steps
            - Any general travel question
            """
    )
    public ResponseEntity<ApiResponse<ChatResponse>> chat(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("Authorization") String authorizationHeader) {

        log.info("POST /ai/chat — user={}, message={}", userDetails.getUsername(), request.getMessage());

        String reply = chatbotService.chat(request.getMessage(), authorizationHeader, request.getSessionId());

        ChatResponse chatResponse = ChatResponse.builder()
                .reply(reply)
                .sessionId(request.getSessionId())
                .build();

        return ResponseEntity.ok(ApiResponse.success("AI response generated", chatResponse));
    }
}
