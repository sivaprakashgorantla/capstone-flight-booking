package com.flight.payment_service.service;

import com.flight.payment_service.dto.ConfirmBookingRequest;
import com.flight.payment_service.dto.ServiceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceClient {

    private final RestTemplate restTemplate;

    @Value("${internal.service.key}")
    private String internalServiceKey;

    /**
     * Notifies booking-service to update booking status after payment outcome.
     * Uses Eureka service discovery via @LoadBalanced RestTemplate.
     */
    public void updateBookingStatus(String bookingReference, String paymentReference, String status) {
        String url = "http://BOOKING-SERVICE/bookings/internal/confirm";

        ConfirmBookingRequest body = new ConfirmBookingRequest(bookingReference, paymentReference, status);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Service-Key", internalServiceKey);

        HttpEntity<ConfirmBookingRequest> entity = new HttpEntity<>(body, headers);

        try {
            log.info("Notifying booking-service: ref={}, status={}", bookingReference, status);
            ResponseEntity<ServiceResponse> response = restTemplate.exchange(
                    url, HttpMethod.PATCH, entity,
                    new ParameterizedTypeReference<ServiceResponse>() {}
            );
            if (response.getBody() != null && response.getBody().isSuccess()) {
                log.info("Booking-service updated successfully for ref={}", bookingReference);
            } else {
                log.warn("Booking-service returned non-success for ref={}", bookingReference);
            }
        } catch (Exception e) {
            log.error("Failed to notify booking-service for ref={}: {}", bookingReference, e.getMessage());
            // Do not rethrow — payment is already recorded; booking-service can be retried
        }
    }
}
