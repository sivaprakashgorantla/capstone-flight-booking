package com.flight.ai.client;

import com.flight.ai.dto.PaymentApiResponse;
import com.flight.ai.dto.PaymentRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "PAYMENT-SERVICE")
public interface PaymentServiceClient {

    @PostMapping("/payments/initiate")
    PaymentApiResponse initiatePayment(
            @RequestHeader("Authorization") String authorization,
            @RequestBody PaymentRequest request
    );
}
