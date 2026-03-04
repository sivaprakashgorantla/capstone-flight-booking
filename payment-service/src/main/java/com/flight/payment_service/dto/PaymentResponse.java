package com.flight.payment_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentResponse {
    private Long id;
    private String paymentReference;
    private Long bookingId;
    private String bookingReference;
    private String userId;
    private BigDecimal amount;
    private String paymentMethod;
    private String status;
    private String transactionId;
    private String failureReason;
    private LocalDateTime createdAt;
    private String message;
    private String bookingStatus;     // reflects updated booking status after payment
}
