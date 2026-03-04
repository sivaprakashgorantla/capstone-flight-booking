package com.flight.flight_service.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title       = "Flight Search Service API",
        version     = "1.0.0",
        description = "Search available flights by city, date, and passengers. "
                    + "Guest access allowed for search. "
                    + "Admin operations require a valid JWT from POST /auth/login.",
        contact     = @Contact(name = "POC Microservices", email = "admin@flight.com")
    ),
    servers = {
        @Server(url = "http://localhost:8083", description = "Direct"),
        @Server(url = "http://localhost:8080", description = "Via API Gateway")
    }
)
@SecurityScheme(
    name        = "bearerAuth",
    type        = SecuritySchemeType.HTTP,
    scheme      = "bearer",
    bearerFormat = "JWT",
    in          = SecuritySchemeIn.HEADER,
    description = "Paste JWT token obtained from POST /auth/login (auth-service)"
)
public class SwaggerConfig {
    // Configuration via annotations — no bean registration needed
}
