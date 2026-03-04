package com.flight.booking_service.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PassengerResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private int age;
    private String gender;
    private String seatNumber;
    private String passportNumber;
}
