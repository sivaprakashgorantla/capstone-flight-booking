package com.flight.payment_service.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceResponse<T> {
    private boolean success;
    private String message;
    private T data;
}
