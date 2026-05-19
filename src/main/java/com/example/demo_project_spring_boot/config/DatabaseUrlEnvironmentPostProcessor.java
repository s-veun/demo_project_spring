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
 * Resolves DATABASE_URL/JDBC_DATABASE_URL into Spring datasource properties before auto-configuration.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String datasourceUrl = environment.getProperty("spring.datasource.url");
        if (StringUtils.hasText(datasourceUrl)) {
            return;
        }

        String databaseUrl = firstNonBlank(
                environment.getProperty("JDBC_DATABASE_URL"),
                environment.getProperty("DATABASE_URL")
        );

        if (!StringUtils.hasText(databaseUrl)) {
            return;
        }

        Map<String, Object> overrides = new HashMap<>();

        if (databaseUrl.startsWith("jdbc:")) {
            overrides.put("spring.datasource.url", databaseUrl);
        } else if (databaseUrl.startsWith("postgres://") || databaseUrl.startsWith("postgresql://")) {
            ParsedDatabaseUrl parsed = parseDatabaseUrl(databaseUrl);
            overrides.put("spring.datasource.url", parsed.jdbcUrl());

            if (StringUtils.hasText(parsed.username()) && !StringUtils.hasText(environment.getProperty("spring.datasource.username"))) {
                overrides.put("spring.datasource.username", parsed.username());
            }
            if (StringUtils.hasText(parsed.password()) && !StringUtils.hasText(environment.getProperty("spring.datasource.password"))) {
                overrides.put("spring.datasource.password", parsed.password());
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


