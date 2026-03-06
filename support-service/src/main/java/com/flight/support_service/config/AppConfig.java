package com.flight.support_service.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableAsync
public class AppConfig {

    /**
     * Load-balanced RestTemplate for inter-service calls via Eureka.
     * Usage: restTemplate.postForEntity("http://EMAIL-SERVICE/email/...", ...)
     */
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
