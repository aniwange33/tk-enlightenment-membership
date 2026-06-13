package com.tertech.tkenlightment.membership.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    OpenAPI membershipOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Taraku Enlightenment Club Membership API")
                        .description("Membership management for the Taraku Enlightenment Club")
                        .version("0.0.1"))
                .components(new Components()
                        .addSecuritySchemes(
                                BEARER_SCHEME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description(
                                                "JWT obtained from POST /api/auth/login. "
                                                        + "Paste the token value only (no 'Bearer ' prefix).")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
