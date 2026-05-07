package com.example.demo_project_spring_boot;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class DemoProjectSpringBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoProjectSpringBootApplication.class, args);
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .servers(List.of(
                        // ១. កំណត់ URL របស់ Railway (ប្តូរតាម Domain ជាក់ស្តែងរបស់អ្នក)
                        new Server().url("https://demoprojectspring-production.up.railway.app")
                                .description("Production Server (Railway)"),

                        // ២. កំណត់សម្រាប់តេស្តនៅក្នុងម៉ាស៊ីនផ្ទាល់ខ្លួន
                        new Server().url("http://localhost:8080")
                                .description("Local Development Server")
                ));
    }
}