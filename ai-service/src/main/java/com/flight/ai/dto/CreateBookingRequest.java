package com.flight.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingRequest {
    private Long                  flightId;
    private List<PassengerRequest> passengers;
}
