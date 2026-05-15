# Social Authentication Module (Spring Boot 3 + Security 6)

This module provides production-oriented social authentication with:

- Continue with Google
- Continue with Facebook
- OAuth2 login flow
- JWT access token + refresh token
- Stateless protected APIs
- Secure logout (token revocation)
- Role-based authorization
- User profile API

## Architecture

- **Controller layer**: `AuthenticationController`, `UserProfileController`
- **Service layer**: `AuthenticationServiceImpl`
- **Security layer**:
  - `SecurityConfig`
  - `JwtAuthenticationFilter`
  - `CustomOAuth2UserService`
  - `OAuth2AuthenticationSuccessHandler`
  - `OAuth2AuthenticationFailureHandler`
  - `CustomAuthenticationEntryPoint`
  - `CustomAccessDeniedHandler`
- **Persistence**: `User` entity + `UserRepository`

## OAuth2 Flow (Google/Facebook)

1. Frontend calls `GET /api/v1/auth/oauth2/google` or `GET /api/v1/auth/oauth2/facebook`
2. Backend returns authorization URI (`/oauth2/authorization/{provider}`)
3. Browser redirects user to provider consent screen
4. Provider redirects back to `/login/oauth2/code/{provider}`
5. `CustomOAuth2UserService` normalizes user info and auto-registers/updates user
6. `OAuth2AuthenticationSuccessHandler` generates JWT tokens and persists active session tokens
7. Backend returns JSON token payload or redirects to frontend callback (`app.oauth2.authorized-redirect-uri`)

## API Endpoints

- `GET /api/v1/auth/oauth2/google`
- `GET /api/v1/auth/oauth2/facebook`
- `POST /api/v1/auth/refresh-token`
- `POST /api/v1/auth/logout`
- `GET /api/v1/user/profile`

## Environment Variables

```bash
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
GOOGLE_REDIRECT_URI=http://localhost:8080/login/oauth2/code/google

FACEBOOK_APP_ID=
FACEBOOK_APP_SECRET=
FACEBOOK_REDIRECT_URI=http://localhost:8080/login/oauth2/code/facebook

JWT_SECRET=
JWT_ACCESS_TOKEN_EXPIRATION=3600000
JWT_REFRESH_TOKEN_EXPIRATION=2592000000

SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/postgres
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres

OAUTH2_AUTHORIZED_REDIRECT_URI=http://localhost:3000/auth/callback
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:3001
```

## Postman Testing

1. Use `GET /api/v1/auth/oauth2/google` and open the returned URL in browser.
2. Copy `accessToken` + `refreshToken` from callback/JSON response.
3. Call `GET /api/v1/user/profile` with header `Authorization: Bearer <accessToken>`.
4. Refresh access token using `POST /api/v1/auth/refresh-token`.
5. Logout using `POST /api/v1/auth/logout` with bearer token and optional refresh token body.

## Security Notes

- Access token validation enforces token type `ACCESS`.
- Refresh token endpoint enforces token type `REFRESH` and checks token against active DB session.
- Logout revokes both access and refresh tokens.
- APIs remain stateless (`SessionCreationPolicy.STATELESS`).

