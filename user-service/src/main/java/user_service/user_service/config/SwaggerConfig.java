package user_service.user_service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, bearerSecurityScheme())
                );
    }

    private Info apiInfo() {
        return new Info()
                .title("User Service API")
                .description("""
                        User Management Service — handles profile, role, password and account operations.
                        Tokens are issued by auth-service. Paste your Bearer token above to authorize.
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("POC Microservices")
                        .email("support@flight-poc.com"));
    }

    private SecurityScheme bearerSecurityScheme() {
        return new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Paste the JWT token received from POST /auth/login");
    }
}
