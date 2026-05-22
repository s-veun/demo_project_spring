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
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.List;

@Configuration
public class OpenAPIConfig {

    @Value("${app.api.url:}")
    private String apiUrl;

    // ✅ លុប static block SpringDocUtils.replaceWithClass ចេញទាំងស្រុង
    // ព្រោះវាបំផ្លាញ List<MultipartFile> rendering

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        // ✅ Schema ត្រឹមត្រូវ: type=string, format=binary
        // OpenAPI 3.0 specification តម្រូវឱ្យប្រើបែបនេះសម្រាប់ file upload

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

        List<Server> servers = buildServers();

        return new OpenAPI()
                .info(new Info()
                        .title("E-Commerce API")
                        .version("1.0.0")
                        .description("""
                                ## Complete E-Commerce REST API
                                """)
                        .contact(new Contact()
                                .name("API Support")
                                .email("support@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(servers)
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

    @Bean
    public GroupedOpenApi adminApiGroup() {
        return GroupedOpenApi.builder()
                .group("admin")
                .pathsToMatch("/api/v1/admin/**")
                .build();
    }

    private List<Server> buildServers() {
        Server sameOriginServer = new Server()
                .url("/")
                .description("Current host (recommended for Swagger Try it out)");

        if (!StringUtils.hasText(apiUrl)) {
            return List.of(sameOriginServer);
        }

        String normalizedApiUrl = apiUrl.trim();
        if ("/".equals(normalizedApiUrl)) {
            return List.of(sameOriginServer);
        }

        return List.of(
                sameOriginServer,
                new Server()
                        .url(normalizedApiUrl)
                        .description("Primary API Server")
        );
    }
}

