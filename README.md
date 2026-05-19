# demo_project_spring_boot (Railway Only)

Production-ready Spring Boot backend for deployment on Railway only.

## Tech Stack

- Java 21
- Spring Boot 3.4.1
- Gradle
- Railway PostgreSQL
- Spring Security + JWT
- OAuth2 Google/Facebook (optional, auto-disabled when credentials are missing)
- Cloudinary
- Swagger/OpenAPI

## 1) Required Environment Variables

Copy from `.env.example` and set values in Railway Project Variables.

Required:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`
- `CLOUDINARY_CLOUD_NAME`
- `CLOUDINARY_API_KEY`
- `CLOUDINARY_API_SECRET`

Recommended:

- `SPRING_PROFILES_ACTIVE=railway`
- `APP_SECURITY_STRICT_STARTUP_VALIDATION=true`
- `FRONTEND_BASE_URL`
- `GOOGLE_CLIENT_ID` (optional for social login)
- `GOOGLE_CLIENT_SECRET` (optional for social login)

Core runtime:

- `PORT=8080` (Railway injects this automatically)

## 2) Local Verification

```bash
cd /Users/ppc/Desktop/ecommerce_app/demo_project_spring_boot
./gradlew clean bootJar
```

```bash
cd /Users/ppc/Desktop/ecommerce_app/demo_project_spring_boot
./gradlew bootRun
```

## 3) Railway Deploy (CLI)

```bash
cd /Users/ppc/Desktop/ecommerce_app/demo_project_spring_boot
railway login
railway link
./deploy-railway.sh
```

## 4) GitHub Deployment Flow

- Add repository secret: `RAILWAY_TOKEN`
- Push to `main`
- Workflow file: `.github/workflows/railway-deploy.yml`

## 5) Post Deploy Checks

- `https://<railway-domain>/actuator/health`
- `https://<railway-domain>/swagger-ui/index.html`
- `https://<railway-domain>/v3/api-docs`

Google OAuth2 redirect URI must exactly be:

- `https://<railway-domain>/login/oauth2/code/google`

## 6) Production Checklist

- `JWT_SECRET` is at least 32 random chars
- `SPRING_DATASOURCE_URL` is present
- `CORS_ALLOWED_ORIGINS` set to exact frontend domain(s)
- `SPRING_JPA_HIBERNATE_DDL_AUTO` set appropriately (`validate` recommended after schema stabilization)
- OAuth2 credentials configured only if social login is needed
- Cloudinary credentials valid

## 7) Troubleshooting (Railway)

- **App fails at startup with JWT error**: set valid `JWT_SECRET`
- **DB connection failure**: verify `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- **OAuth2 disabled warning**: expected when `GOOGLE_CLIENT_ID/GOOGLE_CLIENT_SECRET` are absent (app still starts)
- **OAuth2 invalid_client**: verify Google credentials + exact callback URL
- **CORS blocked requests**: update `CORS_ALLOWED_ORIGINS` with your frontend domain

## References

- `docs/JWT_RAILWAY_SETUP.md`
- `docs/GOOGLE_OAUTH2_SETUP_RAILWAY.md`
- `docs/RAILWAY_E2E_TESTING.md`

