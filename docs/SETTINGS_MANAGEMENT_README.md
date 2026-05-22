# Admin Settings Management System

This module provides a dynamic, secure, and frontend-ready settings system for the admin dashboard.

## Base Path

- `/api/v1/admin/settings`

## Categories

- `general`
- `security`
- `email`
- `payment`
- `notification`
- `seo`
- `social`
- `theme`

## Key Features

- Role-protected admin APIs via `@PreAuthorize("hasRole('ADMIN')")`
- Dynamic key-value storage with JSON value support
- Tenant/locale-aware settings (`tenant`, `locale` query params)
- Audit trail in `setting_audit_logs`
- Caching with Spring Cache (`simple` default, Redis-ready)
- Logo/favicon upload with file type and size validation
- Backup/restore/import endpoints
- Test email endpoint using stored SMTP settings

## Core Endpoints

- `GET/PUT /general`
- `GET/PUT /security`
- `GET/PUT /email`
- `POST /email/test`
- `GET/PUT /payment`
- `GET/PUT /notification`
- `GET/PUT /seo`
- `GET/PUT /social`
- `GET/PUT /theme`
- `POST /upload/logo`
- `POST /upload/favicon`
- `GET /system-info`
- `GET /health`
- `GET /backup`
- `POST /restore`
- `POST /import`

## Data Model

- `settings` table stores key-value settings per category
- `setting_audit_logs` table stores every create/update activity

## Quick Verify

```zsh
cd /Users/ppc/Desktop/ecommerce_app/demo_project_spring_boot
./gradlew test --no-daemon
```

## Production Notes

- Set `SPRING_CACHE_TYPE=redis` with Redis host/port for distributed cache.
- Keep SMTP secrets and payment secrets in secure secret storage.
- Restrict Cloudinary credentials and folder-level policies.

