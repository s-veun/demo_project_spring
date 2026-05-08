package com.example.demo_project_spring_boot.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BinarySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Configuration
public class OpenAPIConfig {

    static {
        // ← នេះជា key fix: បង្ខំ Swagger UI render MultipartFile ជា file picker button
        SpringDocUtils.getConfig()
                .replaceWithClass(MultipartFile.class, org.springframework.core.io.Resource.class);
    }

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("E-Commerce API")
                        .version("1.0.0")
                        .description("""
                                ## 🚀 Complete E-Commerce REST API
                                
                                **សមត្ថភាពសំខាន់ៗ:**
                                - User Authentication & Authorization (JWT)
                                - Product Management with Cloudinary
                                - Shopping Cart & Orders
                                - Reviews & Ratings
                                - Wishlist & Popularity Tracking
                                
                                **របៀបប្រើប្រាស់ Authentication:**
                                1. Login តាមរយៈ `POST /api/v1/login`
                                2. ចម្លង (Copy) Token ចេញពី Response
                                3. ចុចប៊ូតុង **Authorize** ខាងលើ
                                4. វាយពាក្យ: `Bearer <your-token>`
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
                        .addSchemas("FileUpload", new Schema<>()
                                .type("object")
                                .addProperty("file", new BinarySchema()
                                        .description("Image file to upload"))
                                .addProperty("folder", new StringSchema()
                                        ._default("uploads")
                                        .description("Destination folder")))
                        .addSchemas("MultipleFileUpload", new Schema<>()
                                .type("object")
                                .addProperty("files", new ArraySchema()
                                        .items(new BinarySchema()
                                                .description("Image files")))
                                .addProperty("folder", new StringSchema()
                                        ._default("uploads")
                                        .description("Destination folder")))
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("សូមបញ្ចូលតែ Token (មិនបាច់ថែមពាក្យ Bearer ពីមុខទេ)")));
    }
}