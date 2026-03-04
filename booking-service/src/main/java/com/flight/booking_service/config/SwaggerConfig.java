package com.flight.booking_service.config;

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
        title       = "Booking Service API",
        version     = "1.0.0",
        description = "Flight booking — select flight, enter passenger details, auto-assign seats, "
                    + "initiate payment. Requires JWT from POST /auth/login.",
        contact     = @Contact(name = "POC Microservices", email = "admin@flight.com")
    ),
    servers = {
        @Server(url = "http://localhost:8084", description = "Direct"),
        @Server(url = "http://localhost:8080", description = "Via API Gateway")
    }
)
@SecurityScheme(
    name         = "bearerAuth",
    type         = SecuritySchemeType.HTTP,
    scheme       = "bearer",
    bearerFormat = "JWT",
    in           = SecuritySchemeIn.HEADER,
    description  = "Paste JWT from POST /auth/login"
)
public class SwaggerConfig {}
