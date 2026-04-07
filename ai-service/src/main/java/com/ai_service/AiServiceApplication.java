package com.ai_service;

/**
 * Legacy entry point — delegates to the correct application class.
 * Do NOT add @SpringBootApplication here — use com.flight.ai.AiServiceApplication.
 */
public class AiServiceApplication {

    public static void main(String[] args) {
        com.flight.ai.AiServiceApplication.main(args);
    }
}
