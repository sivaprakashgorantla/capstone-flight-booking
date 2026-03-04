package com.flight.booking_service.service;

import com.flight.booking_service.dto.FlightDetailsDTO;
import com.flight.booking_service.dto.ServiceResponse;
import com.flight.booking_service.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlightServiceClient {

    private final RestTemplate restTemplate;

    public FlightDetailsDTO getFlightDetails(Long flightId) {
        String url = "http://FLIGHT-SERVICE/flights/" + flightId;
        log.info("Fetching flight details from flight-service: flightId={}", flightId);
        try {
            ResponseEntity<ServiceResponse<FlightDetailsDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ServiceResponse<FlightDetailsDTO>>() {}
            );
            if (response.getBody() == null || !response.getBody().isSuccess()) {
                throw new BadRequestException("Flight not found: " + flightId);
            }
            return response.getBody().getData();
        } catch (HttpClientErrorException.NotFound e) {
            throw new BadRequestException("Flight not found with id: " + flightId);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch flight details: {}", e.getMessage());
            throw new BadRequestException("Flight service unavailable. Please try again later.");
        }
    }
}
