package com.stok.anandam.store.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String bearerAuth = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Store Anandam API")
                        .version("1.0")
                        .description("API untuk aplikasi Stok Anandam: auth, users, purchase, sales, stock, canvasing, TKDN, migrasi, dashboard.")
                        .contact(new Contact().name("Store Anandam")))
                .addSecurityItem(new SecurityRequirement().addList(bearerAuth))
                .components(new Components()
                        .addSecuritySchemes(bearerAuth,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Login via POST /api/v1/auth/login, lalu isi token di sini.")));
    }
}
