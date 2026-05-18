# Fly.io Deployment Guide (Docker + Spring Boot + Postgres)

## 1) Prerequisites
- Fly CLI installed and authenticated (`fly auth whoami`)
- Docker running locally
- Project root: `demo_project_spring_boot`

## 2) Build verification
```bash
./gradlew --no-daemon clean test

docker build -t demo-project-spring-boot:fly .
```

## 3) Fly app initialization
```bash
fly launch --name demo-project-spring-boot --region sin --no-deploy
```

## 4) Create and attach PostgreSQL
```bash
fly postgres create \
  --name demo-project-spring-boot-db \
  --region sin \
  --vm-size shared-cpu-1x \
  --vm-memory 512 \
  --initial-cluster-size 1 \
  --volume-size 5

fly postgres attach demo-project-spring-boot-db \
  --app demo-project-spring-boot \
  --database-name demo_project_spring_boot \
  --database-user demo_project_spring_boot \
  --yes
```

## 5) Set secrets
```bash
fly secrets set \
  DB_HOST="demo-project-spring-boot-db.internal" \
  DB_PORT="5432" \
  DB_NAME="demo_project_spring_boot" \
  DB_USERNAME="demo_project_spring_boot" \
  DB_PASSWORD="<db-password>" \
  JWT_SECRET="<min-32-char-random-secret>" \
  GOOGLE_CLIENT_ID="<google-client-id>" \
  GOOGLE_CLIENT_SECRET="<google-client-secret>" \
  FRONTEND_URL="https://your-frontend-domain.com" \
  OAUTH2_AUTHORIZED_REDIRECT_URI="https://your-frontend-domain.com/auth/success" \
  OAUTH2_ALLOWED_REDIRECT_URIS="https://your-frontend-domain.com/auth/success" \
  CORS_ALLOWED_ORIGINS="https://your-frontend-domain.com" \
  --app demo-project-spring-boot
```

## 6) Deploy
```bash
fly deploy --app demo-project-spring-boot
```

## 7) Verify
```bash
fly status --app demo-project-spring-boot
fly logs --app demo-project-spring-boot
fly checks list --app demo-project-spring-boot
```

Then open:
- `https://demo-project-spring-boot.fly.dev/swagger-ui.html`
- `https://demo-project-spring-boot.fly.dev/actuator/health`

## Troubleshooting
- **Container exits quickly**: Check startup logs (`fly logs`) and validate `JWT_SECRET`, `SPRING_PROFILES_ACTIVE=prod`, and DB secrets.
- **Port binding errors**: Ensure app uses `server.port=${PORT:8080}` and `fly.toml` uses `internal_port = 8080`.
- **DB connection refused**: Confirm Postgres app is healthy, `DB_HOST` is internal hostname, and app is attached.
- **Health checks failing**: Ensure actuator health endpoint is exposed and readiness path is `/actuator/health/readiness`.
- **OAuth redirect mismatch**: Update Google OAuth Authorized redirect URI to `https://demo-project-spring-boot.fly.dev/login/oauth2/code/google`.
- **OOM / memory pressure**: Increase Fly VM memory to 2GB and redeploy (`fly scale memory 2048 --app demo-project-spring-boot`).

