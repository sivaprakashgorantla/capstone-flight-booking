package com.flight.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentInfo {
    private Long          id;
    private String        paymentReference;
    private String        bookingReference;
    private String        status;           // SUCCESS / FAILED
    private BigDecimal    amount;
    private String        paymentMethod;
    private LocalDateTime processedAt;
}
