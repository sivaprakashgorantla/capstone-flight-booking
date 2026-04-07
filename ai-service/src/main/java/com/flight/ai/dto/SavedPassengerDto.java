package com.flight.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SavedPassengerDto {
    private Long   id;
    private String label;
    private String firstName;
    private String lastName;
    private int    age;
    private String gender;
    private String email;
    private String phone;
    private String passportNumber;
}
