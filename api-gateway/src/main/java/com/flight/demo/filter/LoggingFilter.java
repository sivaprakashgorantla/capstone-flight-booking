package com.flight.demo.filter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class LoggingFilter implements GlobalFilter {

    private static final Logger logger = LogManager.getLogger(LoggingFilter.class);

    @Override
    public Mono<Void> filter(org.springframework.web.server.ServerWebExchange exchange,
                             org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {

        String correlationId = UUID.randomUUID().toString();
        exchange.getRequest().mutate().header("X-Correlation-ID", correlationId).build();

        logger.info("Incoming Request: {} {} | CorrelationId={}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getURI(),
                correlationId);

        return chain.filter(exchange);
    }
}