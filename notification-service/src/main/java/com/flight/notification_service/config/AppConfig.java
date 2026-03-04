package com.flight.notification_service.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    /**
     * Load-balanced RestTemplate for inter-service calls via Eureka.
     * Use http://SERVICE-NAME/path as the base URL.
     * Currently available for future use (e.g. fetching flight/booking details).
     */
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
