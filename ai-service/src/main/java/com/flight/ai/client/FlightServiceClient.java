package com.flight.ai.client;

import com.flight.ai.dto.FlightSearchApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "FLIGHT-SERVICE")
public interface FlightServiceClient {

    @GetMapping("/flights/search")
    FlightSearchApiResponse searchFlights(
            @RequestParam("departureCity")  String departureCity,
            @RequestParam("destinationCity") String destinationCity,
            @RequestParam("travelDate")     String travelDate,
            @RequestParam("passengers")     int passengers
    );
}
