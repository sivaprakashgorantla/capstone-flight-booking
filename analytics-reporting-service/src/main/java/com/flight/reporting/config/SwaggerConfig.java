package com.flight.reporting.config;

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
                title       = "Analytics & Reporting Service API",
                version     = "1.0",
                description = """
                        UC10: Analytics and Reporting.
                        
                        **Workflow:**
                        1. Admin selects a report type
                        2. System retrieves data (live via RestTemplate + seeded baseline)
                        3. Report is generated with full metrics
                        4. Admin views the report (JSON) or downloads it as CSV
                        
                        **Available Reports:**
                        - `BOOKING_SUMMARY` — Total bookings, status breakdown, top routes, monthly trend
                        - `REVENUE_REPORT`  — Revenue by month, route, airline; net after refunds
                        - `FLIGHT_UTILIZATION` — Per-flight occupancy rates and seat fill %
                        - `CANCELLATION_REPORT` — Counts, refund amounts, by reason and time band
                        - `SUPPORT_SUMMARY`  — Ticket metrics by category, status, agent performance
                        - `PAYMENT_REPORT`   — Success/failure rates, by method, failure reasons
                        - `FULL_DASHBOARD`   — All KPIs on one page with drill-down links
                        
                        **Auth:** All endpoints require an Admin JWT (ROLE_ADMIN).
                        Obtain a token from auth-service `POST /auth/login`.
                        """,
                contact = @Contact(name = "Flight App — Analytics Team",
                        email = "analytics@flightapp.com")
        ),
        servers = {
                @Server(url = "http://localhost:8089",
                        description = "Direct (local)"),
                @Server(url = "http://localhost:8080/reports",
                        description = "Via API Gateway")
        }
)
@SecurityScheme(
        name         = "bearerAuth",
        type         = SecuritySchemeType.HTTP,
        scheme       = "bearer",
        bearerFormat = "JWT",
        in           = SecuritySchemeIn.HEADER,
        description  = "Admin JWT from auth-service POST /auth/login (role must be ADMIN)"
)
public class SwaggerConfig {
}
