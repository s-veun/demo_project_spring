package com.example.demo_project_spring_boot.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

// ✅ លុប: import org.springframework.beans.factory.annotation.Value;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("E-Commerce API")
                        .version("1.0.0")
                        .description("""
                                Complete E-Commerce REST API with Spring Boot

                                **Features:**
                                - User Authentication & Authorization (JWT)
                                - Product Management with Cloudinary
                                - Shopping Cart & Orders
                                - Reviews & Ratings
                                - Wishlist & Popularity Tracking
                                - Admin Dashboard Analytics

                                **How to Authenticate:**
                                1. Login via POST /api/v1/login
                                2. Copy the token from response
                                3. Click Authorize button above
                                4. Enter: Bearer <your-token>
                                """)
                        .contact(new Contact()
                                .name("API Support")
                                .email("support@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url("https://demoprojectspring-production.up.railway.app")
                                .description("🚀 Production Server (Railway)"),
                        new Server()
                                .url("http://localhost:8080")
                                .description("💻 Local Development Server")))
                .addSecurityItem(new SecurityRequirement()
                        .addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token (without Bearer prefix)")));
    }
}