package com.flight.reporting.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    /**
     * Load-balanced RestTemplate for UC10 Step 2 — fetching live data
     * from booking-service, payment-service, cancellation-service, support-service.
     * Uses Eureka service-name resolution: http://SERVICE-NAME/path
     */
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
