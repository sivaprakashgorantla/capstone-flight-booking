package com.flight.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlightSearchData {
    private String         departureCity;
    private String         destinationCity;
    private String         travelDate;
    private int            passengers;
    private int            totalFlights;
    private List<FlightInfo> flights;
}
