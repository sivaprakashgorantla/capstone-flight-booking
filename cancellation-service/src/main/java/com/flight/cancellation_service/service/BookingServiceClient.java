package com.flight.cancellation_service.service;

import com.flight.cancellation_service.dto.ApiResponse;
import com.flight.cancellation_service.dto.BookingDetailsDTO;
import com.flight.cancellation_service.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Calls booking-service internal endpoints using X-Service-Key header.
 * Uses Eureka service discovery via @LoadBalanced RestTemplate.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceClient {

    private final RestTemplate restTemplate;

    @Value("${internal.service.key}")
    private String serviceKey;

    // ── GET booking details ───────────────────────────────────────────────────

    public BookingDetailsDTO getBookingDetails(Long bookingId) {
        String url = "http://BOOKING-SERVICE/bookings/internal/" + bookingId;
        log.info("Fetching booking details from booking-service: bookingId={}", bookingId);
        try {
            HttpHeaders headers = buildHeaders();
            ResponseEntity<ApiResponse<BookingDetailsDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<ApiResponse<BookingDetailsDTO>>() {}
            );
            if (response.getBody() == null || !response.getBody().isSuccess()) {
                throw new BadRequestException("Booking not found: " + bookingId);
            }
            log.info("Booking details fetched: bookingId={}, status={}",
                    bookingId, response.getBody().getData().getStatus());
            return response.getBody().getData();
        } catch (HttpClientErrorException.NotFound e) {
            throw new BadRequestException("Booking not found with id: " + bookingId);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch booking details: {}", e.getMessage());
            throw new BadRequestException(
                    "Booking service unavailable. Please try again later.");
        }
    }

    // ── PATCH cancel booking ──────────────────────────────────────────────────

    public void cancelBooking(Long bookingId) {
        String url = "http://BOOKING-SERVICE/bookings/internal/cancel/" + bookingId;
        log.info("Calling booking-service to cancel bookingId={}", bookingId);
        try {
            HttpHeaders headers = buildHeaders();
            restTemplate.exchange(
                    url,
                    HttpMethod.PATCH,
                    new HttpEntity<>(headers),
                    Void.class
            );
            log.info("Booking {} cancelled via booking-service", bookingId);
        } catch (Exception e) {
            // Log but do NOT rethrow — cancellation record is already saved.
            log.error("Failed to update booking-service after cancellation: {}",
                    e.getMessage());
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
