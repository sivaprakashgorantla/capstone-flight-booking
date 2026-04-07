package com.flight.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {

    @NotBlank(message = "Message cannot be blank")
    private String message;

    // Optional session ID for future conversation history support
    private String sessionId;
}
