package com.flight.support_service.config;

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
                title       = "Support Service API",
                version     = "1.0",
                description = "Customer Support Ticket Management — Use Case 8: Customer Support. " +
                        "Submit and track support tickets for booking issues, payment problems, " +
                        "flight delays, and other inquiries.",
                contact = @Contact(name = "Flight App Support", email = "support@flightapp.com")
        ),
        servers = {
                @Server(url = "http://localhost:8087", description = "Local Development"),
                @Server(url = "http://localhost:8080/support", description = "Via API Gateway")
        }
)
@SecurityScheme(
        name        = "bearerAuth",
        type        = SecuritySchemeType.HTTP,
        scheme      = "bearer",
        bearerFormat = "JWT",
        in          = SecuritySchemeIn.HEADER,
        description = "JWT token obtained from auth-service POST /auth/login"
)
public class SwaggerConfig {
    // All configuration is via annotations above.
}
