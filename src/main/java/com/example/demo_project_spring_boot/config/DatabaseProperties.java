package com.example.demo_project_spring_boot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.Setter;

/**
 * Database configuration properties loaded from application.properties and .env
 * Provides type-safe access to database settings
 */
@Component
@Configuration
@ConfigurationProperties(prefix = "spring.datasource")
@Getter
@Setter
public class DatabaseProperties {
    private String url;
    private String username;
    private String password;
    private String driverClassName;
    private HikariProperties hikari = new HikariProperties();

    @Getter
    @Setter
    public static class HikariProperties {
        private int maximumPoolSize = 10;
        private int minimumIdle = 5;
        private long connectionTimeout = 30000;
        private long idleTimeout = 600000;
        private long maxLifetime = 1800000;
        private String connectionTestQuery;
    }

    @Override
    public String toString() {
        return String.format(
            "DatabaseProperties{" +
            "url='%s', " +
            "username='%s', " +
            "driverClassName='%s', " +
            "hikari=%s" +
            "}",
            maskSensitive(url),
            username,
            driverClassName,
            hikari
        );
    }

    private static String maskSensitive(String value) {
        if (value == null) return "null";
        // Show only first 20 chars and last 10 chars
        if (value.length() > 30) {
            return value.substring(0, 20) + "..." + value.substring(value.length() - 10);
        }
        return value.replaceAll("(?<=.{3}).", "*");
    }
}
