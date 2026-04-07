package com.flight.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SavedPassengerApiResponse {
    private boolean success;
    private String  message;
    private List<SavedPassengerDto> data;
}
