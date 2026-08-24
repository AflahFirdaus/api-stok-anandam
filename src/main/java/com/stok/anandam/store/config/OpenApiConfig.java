package com.stok.anandam.store.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String bearerAuth = "bearerAuth";

        // Server Local untuk development
        Server localServer = new Server()
                .url("http://localhost:9090")
                .description("Local Server (Development)");

        // Server Internal Kantor (IP Local)
        Server internalServer = new Server()
                .url("http://192.168.1.176:9090")
                .description("Internal Server (Kantor)");

        return new OpenAPI()
                .servers(List.of(localServer, internalServer))
                .info(new Info()
                        .title("Store Anandam API")
                        .version("1.0")
                        .description("API untuk aplikasi Stok Anandam - Hanya akses localhost / IP Internal Kantor")
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