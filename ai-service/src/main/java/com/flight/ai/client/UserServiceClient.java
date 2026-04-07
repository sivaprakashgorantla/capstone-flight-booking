package com.flight.ai.client;

import com.flight.ai.dto.SavedPassengerApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "USER-SERVICE")
public interface UserServiceClient {

    @GetMapping("/users/passengers")
    SavedPassengerApiResponse getSavedPassengers(
            @RequestHeader("Authorization") String authorization);
}
