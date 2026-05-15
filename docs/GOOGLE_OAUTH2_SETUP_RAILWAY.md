# Google OAuth2 Setup (Spring Boot 3 + Railway)

This guide fixes the common Google error:

- `Access blocked: Authorization Error`
- `Error 401: invalid_client`
- `The OAuth client was not found`

## 1) Google Cloud Console Setup

1. Open Google Cloud Console: https://console.cloud.google.com/
2. Create/select a project.
3. Go to **APIs & Services** -> **OAuth consent screen**.
4. Configure app info (name, support email, developer email).
5. Add scopes:
   - `.../auth/userinfo.email`
   - `.../auth/userinfo.profile`
   - `openid`
6. If app is in **Testing**, add test users.

## 2) Create OAuth Client ID

1. Go to **APIs & Services** -> **Credentials**.
2. Click **Create credentials** -> **OAuth client ID**.
3. Application type: **Web application**.
4. Add authorized redirect URIs:
   - Local: `http://localhost:8080/login/oauth2/code/google`
   - Production: `https://demoprojectspring-production.up.railway.app/login/oauth2/code/google`

> Redirect URI must match exactly (scheme, domain, path, and trailing slash behavior).

## 3) Enable Required APIs

Enable at least:

- Google People API (recommended)
- Google OAuth2 APIs (if visible in your project/API library)

## 4) Backend Configuration

### application.properties

```properties
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID:}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET:}
spring.security.oauth2.client.registration.google.scope=profile,email
spring.security.oauth2.client.registration.google.redirect-uri=${GOOGLE_REDIRECT_URI:http://localhost:8080/login/oauth2/code/google}

app.oauth2.authorized-redirect-uri=${OAUTH2_AUTHORIZED_REDIRECT_URI:http://localhost:3000/auth/callback}
```

### Security URLs

Public OAuth2/Social login endpoints:

- `/oauth2/**`
- `/login/**`
- `/api/v1/auth/**` (login/register/refresh)

Protected endpoints:

- `/api/v1/users/**`
- `/api/v1/admin/**`

## 5) Railway Environment Variables

Set these variables in Railway service settings:

- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`
- `GOOGLE_REDIRECT_URI` = `https://demoprojectspring-production.up.railway.app/login/oauth2/code/google`
- `OAUTH2_AUTHORIZED_REDIRECT_URI` = `https://your-frontend-domain/auth/callback`
- `JWT_SECRET` (strong secret, 32+ bytes)
- `JWT_SECRET_FORMAT` = `plain` (or `base64` if you provide base64 secret)
- `CORS_ALLOWED_ORIGINS` = `https://your-frontend-domain`

## 6) Frontend Integration

Use these start URLs:

- Google: `/oauth2/authorization/google`
- Facebook: `/oauth2/authorization/facebook`

After success, backend redirects to frontend callback with:

- `accessToken`
- `refreshToken`
- `provider`

Example callback URL:

```text
https://your-frontend-domain/auth/callback?accessToken=...&refreshToken=...&provider=GOOGLE
```

## 7) JWT + User Persistence Flow

Implemented flow:

1. Spring Security OAuth2 login succeeds.
2. `CustomOAuth2UserService` creates/updates user in DB.
3. `OAuth2AuthenticationSuccessHandler` generates JWT access/refresh tokens.
4. Backend redirects to frontend callback URI with token payload.
5. Frontend stores session and uses refresh endpoint when access token expires.

Saved user fields include:

- `id`
- `username`
- `email`
- `provider` (`GOOGLE`)
- `providerId`
- `profileImageUrl`
- `role`
- `createdAt`

## 8) Common invalid_client Root Causes

- Wrong `GOOGLE_CLIENT_ID`
- Wrong `GOOGLE_CLIENT_SECRET`
- Missing Railway env vars
- Redirect URI not whitelisted in Google client
- OAuth client created in another Google project
- Deleted/rotated credentials but old values still deployed

## 9) Quick Verification Checklist

1. Startup logs should show OAuth2 configured and redirect URI values.
2. Open: `https://demoprojectspring-production.up.railway.app/oauth2/authorization/google`
3. Complete Google auth.
4. Verify redirect to frontend callback.
5. Confirm frontend receives tokens and enters authenticated session.

## 10) Local Verification Commands

```bash
cd /Users/ppc/Desktop/ecommerce_app/demo_project_spring_boot
./gradlew clean build
./gradlew bootRun
```

Then open:

- `http://localhost:8080/oauth2/authorization/google`

