package com.flight.notification_service.config;

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
                title       = "Notification Service API",
                version     = "1.0",
                description = "UC9: Notifications and Alerts — " +
                        "Subscribe to notifications, receive booking confirmations, " +
                        "delay alerts, pre-departure reminders and manage preferences.",
                contact = @Contact(name = "Flight App Team", email = "dev@flightapp.com")
        ),
        servers = {
                @Server(url = "http://localhost:8088",      description = "Direct (local)"),
                @Server(url = "http://localhost:8080/notifications", description = "Via API Gateway")
        }
)
@SecurityScheme(
        name         = "bearerAuth",
        type         = SecuritySchemeType.HTTP,
        scheme       = "bearer",
        bearerFormat = "JWT",
        in           = SecuritySchemeIn.HEADER,
        description  = "JWT from auth-service POST /auth/login"
)
@SecurityScheme(
        name        = "serviceKey",
        type        = SecuritySchemeType.APIKEY,
        in          = SecuritySchemeIn.HEADER,
        paramName   = "X-Service-Key",
        description = "Internal service-to-service key (poc-internal-svc-key-2026)"
)
public class SwaggerConfig {
}
