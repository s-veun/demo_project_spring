package com.example.demo_project_spring_boot.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Validates database connectivity on application startup
 * Fails fast if database is unreachable instead of waiting for first request
 * 
 * This component runs AFTER all beans are initialized but BEFORE the application
 * starts accepting requests.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseHealthCheck implements ApplicationRunner {

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🔍 Starting Database Connectivity Check...");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        try (Connection connection = dataSource.getConnection()) {
            // Test basic connectivity
            log.info("✅ Database connection established successfully!");

            DatabaseMetaData metaData = connection.getMetaData();
            String databaseProductName = metaData.getDatabaseProductName();
            String databaseProductVersion = metaData.getDatabaseProductVersion();
            String databaseUrl = metaData.getURL();
            String databaseUser = metaData.getUserName();

            log.info("📊 Database Product: {}", databaseProductName);
            log.info("📊 Database Version: {}", databaseProductVersion);
            log.info("🔗 Database URL: {}", databaseUrl);
            log.info("👤 Database User: {}", databaseUser);

            // Run a simple portable validation query.
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery("SELECT 1")) {
                    if (resultSet.next()) {
                        log.info("✅ Validation query succeeded.");
                    }
                }
            }

            if (databaseProductName != null && databaseProductName.toLowerCase().contains("postgresql")) {
                try (Statement statement = connection.createStatement()) {
                    try (ResultSet resultSet = statement.executeQuery(
                        "SELECT count(*) as connection_count FROM pg_stat_activity WHERE datname = current_database()")) {
                        if (resultSet.next()) {
                            int connectionCount = resultSet.getInt("connection_count");
                            log.info("🔗 Active Connections: {}", connectionCount);
                        }
                    }
                } catch (Exception ex) {
                    log.warn("Skipping PostgreSQL connection-count query: {}", ex.getMessage());
                }

                try (Statement statement = connection.createStatement()) {
                    try (ResultSet resultSet = statement.executeQuery(
                        "SELECT current_database(), current_user, now()")) {
                        if (resultSet.next()) {
                            String database = resultSet.getString(1);
                            String user = resultSet.getString(2);
                            String timestamp = resultSet.getString(3);
                            log.info("📍 Current Database: {}", database);
                            log.info("👤 Current User: {}", user);
                            log.info("⏰ Server Time: {}", timestamp);
                        }
                    }
                }
            }

            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("✅ Database Health Check: PASSED");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        } catch (Exception e) {
            log.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.error("❌ Database Health Check: FAILED");
            log.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.error("🔴 ERROR: Cannot connect to database!");
            log.error("Error Type: {}", e.getClass().getSimpleName());
            log.error("Error Message: {}", e.getMessage());

            // Print root cause
            Throwable cause = e.getCause();
            int depth = 1;
            while (cause != null && depth < 5) {
                log.error("  └─ Caused by ({}): {} - {}", depth, cause.getClass().getSimpleName(), cause.getMessage());
                cause = cause.getCause();
                depth++;
            }

            log.error("");
            log.error("🔧 TROUBLESHOOTING STEPS:");
            log.error("  1. Verify PostgreSQL is running:");
            log.error("     - Docker: docker-compose -f docker-compose.dev.yml up -d");
            log.error("     - macOS: brew services start postgresql@15");
            log.error("     - Linux: sudo systemctl start postgresql");
            log.error("");
            log.error("  2. Check JDBC URL in .env file:");
            log.error("     - Should contain: ?sslmode=disable (for local dev)");
            log.error("     - Example: jdbc:postgresql://localhost:5432/ecommerce_db?sslmode=disable");
            log.error("");
            log.error("  3. Verify credentials in .env:");
            log.error("     - SPRING_DATASOURCE_USERNAME");
            log.error("     - SPRING_DATASOURCE_PASSWORD");
            log.error("");
            log.error("  4. Check database exists:");
            log.error("     - psql -U postgres -c '\\l' | grep ecommerce_db");
            log.error("");
            log.error("  5. Test connection directly:");
            log.error("     - psql -h localhost -U postgres -d ecommerce_db");
            log.error("");
            log.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            // Throw to prevent application startup
            throw new RuntimeException("Database connectivity check failed. Application will not start.", e);
        }
    }
}
