package com.example.demo_project_spring_boot.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {

    // ✅ លុប static block SpringDocUtils.replaceWithClass ចេញទាំងស្រុង
    // ព្រោះវាបំផ្លាញ List<MultipartFile> rendering

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        // ✅ Schema ត្រឹមត្រូវ: type=string, format=binary
        // OpenAPI 3.0 specification តម្រូវឱ្យប្រើបែបនេះសម្រាប់ file upload
        Schema<?> binarySchema = new Schema<>()
                .type("string")
                .format("binary");

        Schema<?> singleFileUploadSchema = new Schema<>()
                .type("object")
                .addProperty("file", new Schema<>()
                        .type("string")
                        .format("binary")
                        .description("រូបភាពដែលត្រូវ upload"))
                .addProperty("folder", new StringSchema()
                        ._default("uploads")
                        .description("Folder គោលដៅ (ស្រេចចិត្ត)"));

        Schema<?> multipleFileUploadSchema = new Schema<>()
                .type("object")
                .addProperty("files", new ArraySchema()
                        .items(new Schema<>()
                                .type("string")
                                .format("binary"))
                        .description("រូបភាពច្រើនដែលត្រូវ upload"))
                .addProperty("folder", new StringSchema()
                        ._default("uploads")
                        .description("Folder គោលដៅ (ស្រេចចិត្ត)"));

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
                        .addSchemas("FileUpload",         singleFileUploadSchema)
                        .addSchemas("MultipleFileUpload", multipleFileUploadSchema)
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("សូមបញ្ចូលតែ Token (មិនបាច់ថែមពាក្យ Bearer ពីមុខទេ)")));
    }
}