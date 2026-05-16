# Google/Facebook OAuth2 Production Implementation (Spring Boot 3 + Railway + Next.js)

This runbook fixes `redirect_uri_mismatch` issues and documents end-to-end Google/Facebook login with JWT.

## 1) Google Cloud Console (must match exactly)

Create a **Web application** OAuth client and configure:

- Authorized JavaScript origins
  - `http://localhost:3000`
  - `https://your-frontend-domain.com`
- Authorized redirect URIs
  - `http://localhost:8080/login/oauth2/code/google`
  - `https://demoprojectspring-production.up.railway.app/login/oauth2/code/google`

Important:
- Redirect URI comparison is exact (scheme, host, port, path, trailing slash).
- `.../google` is valid, `.../google/` is different and will fail.

## 1.1) Facebook App Dashboard (must match exactly)

In Meta for Developers:

- Add **Facebook Login** product.
- Add Valid OAuth Redirect URIs:
  - `http://localhost:8080/login/oauth2/code/facebook`
  - `https://demoprojectspring-production.up.railway.app/login/oauth2/code/facebook`
- Turn on **Client OAuth Login** and **Web OAuth Login**.
- If app is in development mode, add test users.

## 2) Backend OAuth2/JWT config (implemented)

Implemented in:
- `src/main/resources/application.properties`
- `src/main/resources/application.yml`
- `src/main/java/com/example/demo_project_spring_boot/config/SecurityConfig.java`
- `src/main/java/com/example/demo_project_spring_boot/security/CustomOAuth2UserService.java`
- `src/main/java/com/example/demo_project_spring_boot/security/OAuth2AuthenticationSuccessHandler.java`
- `src/main/java/com/example/demo_project_spring_boot/security/OAuth2AuthenticationFailureHandler.java`
- `src/main/java/com/example/demo_project_spring_boot/security/OAuth2RedirectUriResolver.java`

Key points:
- Google registration uses Spring Security standard properties.
- Facebook registration uses Spring Security standard properties.
- Redirect template is `{baseUrl}/login/oauth2/code/{registrationId}`.
- `server.forward-headers-strategy=framework` is enabled so Railway forwarded headers produce correct HTTPS callback URI.
- JWT filter skips `/api/v1/auth/**`, `/oauth2/**`, `/login/**`.
- Security permits `/api/v1/auth/**`, `/oauth2/**`, `/login/**`, Swagger docs routes.
- OAuth2 success creates/updates user, generates JWT access+refresh tokens, and redirects frontend.
- Redirect target is validated against `app.oauth2.allowed-redirect-uris` to prevent open redirects.

## 3) Railway environment variables

Set in Railway service variables:

- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`
- `FACEBOOK_APP_ID`
- `FACEBOOK_APP_SECRET`
- `JWT_SECRET`
- `JWT_SECRET_FORMAT=plain` (unless your secret is Base64)
- `FRONTEND_URL=https://your-frontend-domain.com`
- `OAUTH2_AUTHORIZED_REDIRECT_URI=https://your-frontend-domain.com/auth/success`
- `OAUTH2_ALLOWED_REDIRECT_URIS=https://your-frontend-domain.com/auth/success,https://your-frontend-domain.com/auth/callback`
- `CORS_ALLOWED_ORIGINS=https://your-frontend-domain.com,http://localhost:3000`

Notes:
- Keep backend callback in Google Console as Railway backend URL (`/login/oauth2/code/google`).
- Keep backend callback in Facebook app as Railway backend URL (`/login/oauth2/code/facebook`).
- Keep frontend success URL in app env (`OAUTH2_AUTHORIZED_REDIRECT_URI`).

## 4) Frontend flow (Next.js)

Implemented flow:
1. User clicks Google or Facebook login button.
2. Frontend navigates to backend: `/oauth2/authorization/{provider}`.
3. Provider authenticates user.
4. Backend callback receives code at `/login/oauth2/code/{provider}`.
5. Backend generates JWT and redirects to frontend:
   - `/auth/success?token=...&refreshToken=...&provider=GOOGLE|FACEBOOK`
6. Frontend callback page stores session and redirects to dashboard.

Frontend files:
- `table_eco_table_frontend/src/auth/auth-service.ts`
- `table_eco_table_frontend/src/auth/AuthProvider.tsx`
- `table_eco_table_frontend/src/app/auth/callback/OAuthCallbackClient.tsx`

## 5) Debug matrix

### A) `redirect_uri_mismatch`
- Check Google Console redirect URI exact value.
- Check backend domain/protocol in deployed URL.
- Verify `server.forward-headers-strategy=framework` is active.
- Open browser devtools and inspect `redirect_uri` query sent to Google.

### A.1) Facebook `redirect_uri_mismatch`
- Check Facebook redirect URI exact value in Meta dashboard.
- Confirm callback is `/login/oauth2/code/facebook` with no extra trailing slash.

### B) `invalid_client` or `OAuth client was not found`
- Verify `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` in Railway.
- Ensure credentials belong to the same Google project as OAuth app.
- Redeploy after variable changes.

### C) `401 Unauthorized` after OAuth success
- Confirm frontend receives `token` in `/auth/success` URL.
- Verify JWT secret consistency across app instances.
- Verify protected API includes `Authorization: Bearer <accessToken>`.

### D) CORS errors
- Ensure frontend origin is included in `CORS_ALLOWED_ORIGINS`.
- Use explicit origins in production; avoid wildcard.

## 6) Swagger and Postman checks

Swagger:
- Open `/swagger-ui.html`.
- Public routes should be reachable without JWT.
- Protected routes should require bearer token.

Postman quick checks:
1. `GET /api/v1/auth/oauth2/google` -> returns authorization URI metadata.
2. `GET /api/v1/auth/oauth2/facebook` -> returns authorization URI metadata.
3. Browser-only login start: `GET /oauth2/authorization/google` or `GET /oauth2/authorization/facebook`.
4. After OAuth success, copy `token` from frontend callback URL and call protected endpoint:
   - `GET /api/v1/users/...` with `Authorization: Bearer <token>`.

## 7) Local test commands

```zsh
cd /Users/ppc/Desktop/ecommerce_app/demo_project_spring_boot
./gradlew clean test
./gradlew bootRun
```

Then test in browser:
- `http://localhost:8080/oauth2/authorization/google`
- `http://localhost:8080/oauth2/authorization/facebook`
- `http://localhost:3000/login` (frontend social login buttons)

