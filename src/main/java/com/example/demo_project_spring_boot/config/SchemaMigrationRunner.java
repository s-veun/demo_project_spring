package com.example.demo_project_spring_boot.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Lightweight startup migration for OAuth2 columns.
 * This keeps existing environments running when schema is behind the entity model.
 */
@Component
@Order(0)
@RequiredArgsConstructor
@Slf4j
public class SchemaMigrationRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS oauth_provider VARCHAR(50)");
        jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS oauth_provider_id VARCHAR(255)");
        jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS oauth_account_linked BOOLEAN NOT NULL DEFAULT FALSE");

        // OAuth2 users may not have password in legacy schemas.
        try {
            jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN password DROP NOT NULL");
        } catch (Exception ex) {
            log.warn("Skipping password nullability migration (already compatible or unsupported): {}", ex.getMessage());
        }

        // Backfill provider for existing rows to satisfy non-null enum mapping.
        try {
            jdbcTemplate.execute("UPDATE users SET oauth_provider = 'LOCAL' WHERE oauth_provider IS NULL");
        } catch (Exception ex) {
            log.warn("Skipping oauth_provider backfill: {}", ex.getMessage());
        }

        // Backfill from legacy naming if this column already existed in older deployments.
        jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF EXISTS (
                        SELECT 1
                        FROM information_schema.columns
                        WHERE table_schema = current_schema()
                          AND table_name = 'users'
                          AND column_name = 'is_oauth2_linked'
                    ) THEN
                        EXECUTE 'UPDATE users
                                 SET oauth_account_linked = is_oauth2_linked
                                 WHERE oauth_account_linked IS DISTINCT FROM is_oauth2_linked';
                    END IF;
                END $$;
                """);

        try {
            jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN oauth_provider SET DEFAULT 'LOCAL'");
        } catch (Exception ex) {
            log.warn("Skipping oauth_provider default migration: {}", ex.getMessage());
        }

        log.info("OAuth2 schema migration check complete.");
    }
}

