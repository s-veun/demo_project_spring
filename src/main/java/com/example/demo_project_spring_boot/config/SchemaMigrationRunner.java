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
        jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_image_name VARCHAR(255)");
        jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS full_name VARCHAR(255)");
        jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS bio VARCHAR(1200)");
        jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS gender VARCHAR(30)");
        jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS date_of_birth DATE");
        jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS country VARCHAR(100)");
        jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS city VARCHAR(100)");
        jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE");
        jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_verified BOOLEAN NOT NULL DEFAULT FALSE");
        jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified_at TIMESTAMP");
        jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_verified_at TIMESTAMP");
        jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS facebook_url VARCHAR(255)");
        jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS telegram_handle VARCHAR(120)");
        jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS instagram_url VARCHAR(255)");
        jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS linked_in_url VARCHAR(255)");
        jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS email_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE");
        jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS sms_notifications_enabled BOOLEAN NOT NULL DEFAULT FALSE");
        jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS marketing_notifications_enabled BOOLEAN NOT NULL DEFAULT FALSE");
        jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS security_alerts_enabled BOOLEAN NOT NULL DEFAULT TRUE");

        jdbcTemplate.execute("ALTER TABLE addresses ADD COLUMN IF NOT EXISTS country VARCHAR(100)");
        jdbcTemplate.execute("ALTER TABLE addresses ADD COLUMN IF NOT EXISTS state VARCHAR(100)");
        jdbcTemplate.execute("ALTER TABLE addresses ADD COLUMN IF NOT EXISTS postal_code VARCHAR(20)");
        jdbcTemplate.execute("ALTER TABLE addresses ADD COLUMN IF NOT EXISTS address_line1 VARCHAR(255)");
        jdbcTemplate.execute("ALTER TABLE addresses ADD COLUMN IF NOT EXISTS address_line2 VARCHAR(255)");
        jdbcTemplate.execute("ALTER TABLE addresses ADD COLUMN IF NOT EXISTS is_default BOOLEAN NOT NULL DEFAULT FALSE");

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

        // Backfill missing role values to USER for safer RBAC defaults.
        try {
            jdbcTemplate.execute("UPDATE users SET role = 'USER' WHERE role IS NULL");
        } catch (Exception ex) {
            log.warn("Skipping role backfill: {}", ex.getMessage());
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

        try {
            jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN role SET DEFAULT 'USER'");
        } catch (Exception ex) {
            log.warn("Skipping role default migration: {}", ex.getMessage());
        }

        log.info("OAuth2 schema migration check complete.");

        // ── product_images.public_id — allow NULL for pre-uploaded URLs ──────────────
        try {
            jdbcTemplate.execute("ALTER TABLE product_images ALTER COLUMN public_id DROP NOT NULL");
            log.info("product_images.public_id nullable migration applied.");
        } catch (Exception ex) {
            log.debug("product_images.public_id already nullable or migration skipped: {}", ex.getMessage());
        }

        log.info("Schema migration complete.");
    }
}

