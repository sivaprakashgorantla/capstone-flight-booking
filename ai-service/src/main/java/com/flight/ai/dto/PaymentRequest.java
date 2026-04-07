package com.flight.ai.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentRequest {
    private Long       bookingId;
    private String     bookingReference;
    private BigDecimal totalAmount;
    private String     paymentMethod;   // CREDIT_CARD / DEBIT_CARD / UPI / NET_BANKING
    private String     cardLastFour;
    private String     upiId;
    private boolean    simulateFailure = false;
}
