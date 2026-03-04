package com.flight.booking_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new flight booking")
public class CreateBookingRequest {

    @NotNull(message = "Flight ID is required")
    @Schema(description = "ID of the flight to book", example = "1")
    private Long flightId;

    @NotEmpty(message = "At least one passenger is required")
    @Size(max = 9, message = "Maximum 9 passengers per booking")
    @Valid
    @Schema(description = "List of passengers (max 9)")
    private List<PassengerRequest> passengers;
}
