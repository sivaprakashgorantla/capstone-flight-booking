package com.auth_service.flight.demo.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    /**
     * Load-balanced RestTemplate — resolves service names (e.g. "EMAIL-SERVICE")
     * via Eureka service registry instead of a hardcoded host:port.
     * Usage: restTemplate.postForEntity("http://EMAIL-SERVICE/email/welcome", ...)
     */
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
