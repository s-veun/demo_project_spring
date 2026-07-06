package com.example.demo_project_spring_boot.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Resolves hosted Postgres URLs into Spring datasource properties before auto-configuration.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = firstNonBlank(
                environment.getProperty("SPRING_DATASOURCE_URL"),
                environment.getProperty("spring.datasource.url"),
                environment.getProperty("JDBC_DATABASE_URL"),
                environment.getProperty("DATABASE_URL")
        );

        if (!StringUtils.hasText(databaseUrl)) {
            return;
        }

        Map<String, Object> overrides = new HashMap<>();

        String existingDatasourceUsername = environment.getProperty("spring.datasource.username");
        String existingDatasourcePassword = environment.getProperty("spring.datasource.password");

        if (databaseUrl.startsWith("jdbc:")) {
            overrides.put("spring.datasource.url", databaseUrl);
        } else if (databaseUrl.startsWith("postgres://") || databaseUrl.startsWith("postgresql://")) {
            ParsedDatabaseUrl parsed = parseDatabaseUrl(databaseUrl);
            overrides.put("spring.datasource.url", parsed.jdbcUrl());

            String databaseUsername = firstNonBlank(
                    parsed.username(),
                    environment.getProperty("DATABASE_USERNAME"),
                    environment.getProperty("DATABASE_USER"),
                    environment.getProperty("DB_USERNAME")
            );
            String databasePassword = firstNonBlank(
                    parsed.password(),
                    environment.getProperty("DATABASE_PASSWORD"),
                    environment.getProperty("DB_PASSWORD")
            );

            if (StringUtils.hasText(databaseUsername) && !StringUtils.hasText(existingDatasourceUsername)) {
                overrides.put("spring.datasource.username", databaseUsername);
            }
            if (StringUtils.hasText(databasePassword) && !StringUtils.hasText(existingDatasourcePassword)) {
                overrides.put("spring.datasource.password", databasePassword);
            }
        }

        if (!overrides.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource("databaseUrlOverrides", overrides));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private static ParsedDatabaseUrl parseDatabaseUrl(String databaseUrl) {
        URI uri = URI.create(databaseUrl);

        String host = uri.getHost();
        int port = uri.getPort() > 0 ? uri.getPort() : 5432;
        String database = uri.getPath();
        String query = uri.getRawQuery();

        String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + database;
        if (StringUtils.hasText(query)) {
            jdbcUrl += "?" + query;
        }

        String username = null;
        String password = null;
        String userInfo = uri.getRawUserInfo();
        if (StringUtils.hasText(userInfo)) {
            String[] parts = userInfo.split(":", 2);
            username = decode(parts[0]);
            if (parts.length > 1) {
                password = decode(parts[1]);
            }
        }

        return new ParsedDatabaseUrl(jdbcUrl, username, password);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private record ParsedDatabaseUrl(String jdbcUrl, String username, String password) {
    }
}
