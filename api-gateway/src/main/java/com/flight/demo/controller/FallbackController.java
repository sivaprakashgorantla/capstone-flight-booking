package com.flight.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class FallbackController {

    /**
     * Circuit-breaker fallback for ALL HTTP methods (GET, POST, PUT, PATCH, DELETE).
     * Previously only handled GET — causing 405 for POST /bookings, POST /payments/initiate, etc.
     */
    @RequestMapping("/fallback")
    public ResponseEntity<Map<String, Object>> fallback() {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status",    503,
                        "error",     "Service Unavailable",
                        "message",   "The requested service is temporarily unavailable. Please try again in a moment.",
                        "timestamp", LocalDateTime.now().toString()
                ));
    }
}
