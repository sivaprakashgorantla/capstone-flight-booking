package com.flight.reporting.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * RestTemplate-based client for fetching live stats from sibling services.
 * Calls internal/admin endpoints using the shared X-Service-Key header.
 * All calls are gracefully degraded — if a service is unavailable,
 * the analytics service falls back to its seeded data.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LiveDataClient {

    private final RestTemplate restTemplate;

    @Value("${internal.service.key}")
    private String serviceKey;

    // ── Booking Service ───────────────────────────────────────────────────────

    /**
     * Attempts to fetch booking stats from booking-service.
     * Endpoint: GET http://BOOKING-SERVICE/bookings/stats (Admin JWT or service key)
     * Returns null on failure — caller should use seeded data.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> fetchBookingStats() {
        try {
            HttpHeaders headers = buildHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    "http://BOOKING-SERVICE/bookings/stats",
                    HttpMethod.GET, entity, Map.class);
            log.info("[LiveData] Fetched booking stats from booking-service");
            return (Map<String, Object>) response.getBody();
        } catch (Exception e) {
            log.warn("[LiveData] Could not reach booking-service ({}). Using seeded data.", e.getMessage());
            return null;
        }
    }

    // ── Payment Service ───────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public Map<String, Object> fetchPaymentStats() {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());
            ResponseEntity<Map> response = restTemplate.exchange(
                    "http://PAYMENT-SERVICE/payments/stats",
                    HttpMethod.GET, entity, Map.class);
            log.info("[LiveData] Fetched payment stats from payment-service");
            return (Map<String, Object>) response.getBody();
        } catch (Exception e) {
            log.warn("[LiveData] Could not reach payment-service ({}). Using seeded data.", e.getMessage());
            return null;
        }
    }

    // ── Cancellation Service ──────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public Map<String, Object> fetchCancellationStats() {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());
            ResponseEntity<Map> response = restTemplate.exchange(
                    "http://CANCELLATION-SERVICE/cancellations/stats",
                    HttpMethod.GET, entity, Map.class);
            log.info("[LiveData] Fetched cancellation stats from cancellation-service");
            return (Map<String, Object>) response.getBody();
        } catch (Exception e) {
            log.warn("[LiveData] Could not reach cancellation-service ({}). Using seeded data.", e.getMessage());
            return null;
        }
    }

    // ── Support Service ───────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public Map<String, Object> fetchSupportStats() {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());
            ResponseEntity<Map> response = restTemplate.exchange(
                    "http://SUPPORT-SERVICE/support/admin/stats",
                    HttpMethod.GET, entity, Map.class);
            log.info("[LiveData] Fetched support stats from support-service");
            return (Map<String, Object>) response.getBody();
        } catch (Exception e) {
            log.warn("[LiveData] Could not reach support-service ({}). Using seeded data.", e.getMessage());
            return null;
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Service-Key", serviceKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
