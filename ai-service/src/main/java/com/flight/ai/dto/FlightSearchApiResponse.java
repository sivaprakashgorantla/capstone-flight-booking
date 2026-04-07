package com.flight.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlightSearchApiResponse {
    private boolean        success;
    private String         message;
    private FlightSearchData data;
}
