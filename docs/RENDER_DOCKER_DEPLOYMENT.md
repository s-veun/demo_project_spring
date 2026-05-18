# Render Docker Deployment (Spring Boot + PostgreSQL)

## Render Service Settings
- Root Directory: *(leave empty)*
- Dockerfile Path: `Dockerfile`
- Docker Build Context Directory: *(leave empty)*

## Required Environment Variables
- `SPRING_DATASOURCE_URL=jdbc:postgresql://<render-host>:5432/<render-db>?sslmode=require`
- `SPRING_DATASOURCE_USERNAME=<render-username>`
- `SPRING_DATASOURCE_PASSWORD=<render-password>`
- `SPRING_PROFILES_ACTIVE=prod`

## Why JDBC URL Format Matters
Render may expose connection info that looks like `postgres://...`. Spring Boot expects JDBC format for `spring.datasource.url`, so use:
- `jdbc:postgresql://...`

Using non-JDBC URL values can trigger errors such as:
- `Driver claims to not accept jdbcUrl`
- `Failed to determine a suitable driver class`

## Health Check
The app exposes `GET /actuator/health` and Docker health check targets:
- `http://127.0.0.1:${PORT:-8080}/actuator/health`

## Local Validation
```bash
docker build -t springboot-app .
docker run --rm -e PORT=8080 -e SPRING_DATASOURCE_URL="jdbc:postgresql://host.docker.internal:5432/postgres" -e SPRING_DATASOURCE_USERNAME="postgres" -e SPRING_DATASOURCE_PASSWORD="postgres" -e SPRING_PROFILES_ACTIVE="prod" -p 8080:8080 springboot-app
```

Then verify:
```bash
curl http://localhost:8080/actuator/health
```

