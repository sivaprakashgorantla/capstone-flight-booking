package com.flight.booking_service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceResponse<T> {
    private boolean success;
    private String message;
    private T data;
}
