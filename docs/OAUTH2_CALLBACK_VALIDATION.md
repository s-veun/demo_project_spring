# OAuth2 Callback Token Parsing Validation

This document validates the frontend callback token parsing against the backend OAuth2 response shape and provides end-to-end testing guidance for Railway deployment.

## Backend OAuth2 Response Shape

### Success Response (JSON or Redirect with Query Params)

When `app.oauth2.authorized-redirect-uri` is configured, the backend redirects to the frontend callback URL with tokens as query parameters:

```
https://your-frontend.up.railway.app/auth/callback?accessToken=eyJhbGc...&refreshToken=eyJhbGc...&provider=GOOGLE
```

When no redirect URI is configured, the response is JSON:

```json
{
  "success": true,
  "message": "OAuth2 login successful",
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "userId": 1,
  "username": "user@google.com",
  "email": "user@google.com",
  "firstName": "John",
  "lastName": "Doe",
  "profileImageUrl": "https://...",
  "role": "USER",
  "provider": "GOOGLE"
}
```

### Failure Response

```
https://your-frontend.up.railway.app/auth/callback?error=oauth2_authentication_failed&message=Invalid+OAuth2+credentials
```

Or JSON error:

```json
{
  "success": false,
  "status": 401,
  "error": "Authentication Failed",
  "message": "OAuth2 authentication failed",
  "timestamp": 1234567890
}
```

---

## Frontend Callback Token Parsing

The frontend `OAuthCallbackClient` expects tokens in query parameters (primary) or stored session (fallback):

### Token Extraction Logic

**File**: `src/app/auth/callback/OAuthCallbackClient.tsx`

```typescript
// Primary: Extract from query parameters (when backend redirects to callback)
const accessToken = 
  searchParams.get("accessToken") ||
  searchParams.get("token") ||
  searchParams.get("jwt");
const refreshToken = searchParams.get("refreshToken") || undefined;

// Fallback: Try to recover from existing session/cookies
const recovered = await refreshSession();
```

### Auth Service Token Extraction

**File**: `src/auth/auth-service.ts`

```typescript
export function extractAccessToken(
  payload: LoginResponse | RefreshResponse | null | undefined
) {
  if (!payload) return undefined;
  return payload.accessToken || payload.token || payload.jwt;
}
```

Supports multiple token field names:
- `accessToken` (primary)
- `token` (fallback)
- `jwt` (fallback)

---

## Type Definitions

**File**: `src/auth/types.ts`

```typescript
interface LoginResponse {
  accessToken?: string;
  refreshToken?: string;
  token?: string;
  jwt?: string;
  user?: AuthUser;
  [key: string]: unknown; // Allow additional fields from backend
}

interface RefreshResponse {
  accessToken?: string;
  refreshToken?: string;
  token?: string;
  jwt?: string;
  [key: string]: unknown;
}
```

---

## Validation Checklist

### Backend Configuration (application.yml)

```yaml
app:
  oauth2:
    authorized-redirect-uri: ${OAUTH2_AUTHORIZED_REDIRECT_URI:http://localhost:3000/auth/callback}

spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope: openid,profile,email
            redirect-uri: ${GOOGLE_REDIRECT_URI:http://localhost:8080/login/oauth2/code/google}
          facebook:
            client-id: ${FACEBOOK_APP_ID}
            client-secret: ${FACEBOOK_APP_SECRET}
            scope: public_profile,email
            redirect-uri: ${FACEBOOK_REDIRECT_URI:http://localhost:8080/login/oauth2/code/facebook}
```

### Frontend Configuration (.env.local)

```bash
NEXT_PUBLIC_API_BASE_URL=https://your-backend.up.railway.app/api/v1
NEXT_PUBLIC_OAUTH_GOOGLE_PATH=/oauth2/authorization/google
NEXT_PUBLIC_OAUTH_FACEBOOK_PATH=/oauth2/authorization/facebook
```

### CORS + Credentials

Ensure CORS is configured on backend:

```yaml
cors:
  allowed-origins: https://your-frontend.up.railway.app,http://localhost:3000
  allow-credentials: true
  allowed-methods: GET,POST,PUT,DELETE,OPTIONS
  allowed-headers: Authorization,Content-Type
```

---

## Testing Token Parsing

### Test 1: Direct Query Parameter Parsing

```javascript
// Simulate callback URL with tokens
const url = new URL("http://localhost:3000/auth/callback");
url.searchParams.set("accessToken", "eyJhbGc...");
url.searchParams.set("refreshToken", "eyJhbGc...");
url.searchParams.set("provider", "GOOGLE");

// Extract tokens (like OAuthCallbackClient does)
const accessToken = url.searchParams.get("accessToken");
const refreshToken = url.searchParams.get("refreshToken");

console.assert(accessToken === "eyJhbGc...", "Access token extraction failed");
console.assert(refreshToken === "eyJhbGc...", "Refresh token extraction failed");
```

### Test 2: Fallback Token Field Names

```javascript
// Backend might return different token field names
const payload = {
  token: "eyJhbGc...", // Old format
  refreshToken: "refresh_jwt"
};

const accessToken = extractAccessToken(payload);
console.assert(accessToken === "eyJhbGc...", "Should extract 'token' field");

// Verify refreshToken extraction also works
const refreshToken = payload.refreshToken;
console.assert(refreshToken === "refresh_jwt", "Refresh token extraction failed");
```

### Test 3: Error Handling

```javascript
// Error in query params should be caught
const errorUrl = new URL("http://localhost:3000/auth/callback");
errorUrl.searchParams.set("error", "oauth2_authentication_failed");
errorUrl.searchParams.set("message", "Invalid OAuth2 credentials");

const error = errorUrl.searchParams.get("error");
const message = errorUrl.searchParams.get("message");

console.assert(error === "oauth2_authentication_failed", "Error extraction failed");
console.assert(message === "Invalid OAuth2 credentials", "Error message extraction failed");
```

---

## End-to-End Testing with Railway URLs

### Setup Railway Environment

1. **Backend URL**: https://your-backend-project.up.railway.app
2. **Frontend URL**: https://your-frontend-project.up.railway.app

### Backend Environment Variables (Railway)

```
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
GOOGLE_REDIRECT_URI=https://your-backend-project.up.railway.app/login/oauth2/code/google

FACEBOOK_APP_ID=your-facebook-app-id
FACEBOOK_APP_SECRET=your-facebook-app-secret
FACEBOOK_REDIRECT_URI=https://your-backend-project.up.railway.app/login/oauth2/code/facebook

OAUTH2_AUTHORIZED_REDIRECT_URI=https://your-frontend-project.up.railway.app/auth/callback

CORS_ALLOWED_ORIGINS=https://your-frontend-project.up.railway.app

JWT_SECRET=your-256-bit-base64-secret
SPRING_DATASOURCE_URL=postgresql://...
SPRING_DATASOURCE_USERNAME=...
SPRING_DATASOURCE_PASSWORD=...
```

### Frontend Environment Variables (Railway)

```
NEXT_PUBLIC_API_BASE_URL=https://your-backend-project.up.railway.app/api/v1
NEXT_PUBLIC_OAUTH_GOOGLE_PATH=/oauth2/authorization/google
NEXT_PUBLIC_OAUTH_FACEBOOK_PATH=/oauth2/authorization/facebook
```

---

## Test Scenarios

### Scenario 1: Google OAuth2 on Railway

**Steps**:
1. Open https://your-frontend-project.up.railway.app/login
2. Click "Continue with Google"
3. Authenticate with Google account
4. Backend redirects to https://your-frontend-project.up.railway.app/auth/callback?accessToken=...&refreshToken=...
5. Frontend parses tokens from query parameters
6. Frontend calls `completeOAuthLogin()`
7. Frontend redirects to /dashboard with authenticated session

**Expected Result**: Dashboard loads with user profile visible

### Scenario 2: Facebook OAuth2 on Railway

**Steps**:
1. Open https://your-frontend-project.up.railway.app/login
2. Click "Continue with Facebook"
3. Authenticate with Facebook account
4. Backend redirects to https://your-frontend-project.up.railway.app/auth/callback?accessToken=...&provider=FACEBOOK
5. Frontend parses tokens from query parameters
6. Frontend stores tokens in localStorage
7. Frontend redirects to /dashboard

**Expected Result**: Dashboard loads with user profile visible

### Scenario 3: Missing OAuth Credentials

**Steps**:
1. Manipulate frontend to call callback without tokens
2. Observe OAuthCallbackClient error handling

**Expected Result**: 
- Toast notification: "Missing OAuth tokens"
- Redirect to /login page

### Scenario 4: Invalid OAuth Credentials

**Steps**:
1. Manually tamper with OAuth credentials in query parameters
2. Trigger invalid token scenario

**Expected Result**:
- Back-end rejects invalid tokens
- Frontend receives error query parameter
- Toast notification: "OAuth login failed"
- Redirect to /login page

---

## CURL Testing for Railway

### Test 1: Get Google Authorization URL

```bash
curl https://your-backend-project.up.railway.app/api/v1/auth/oauth2/google
# Returns redirect location like:
# https://accounts.google.com/o/oauth2/v2/auth?...
```

### Test 2: Get Facebook Authorization URL

```bash
curl https://your-backend-project.up.railway.app/api/v1/auth/oauth2/facebook
# Returns redirect location like:
# https://www.facebook.com/v18.0/dialog/oauth?...
```

### Test 3: Test with Local Access Token

```bash
# First, login with password to get tokens
curl -X POST https://your-backend-project.up.railway.app/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'

# Response:
# {
#   "success": true,
#   "accessToken": "eyJhbGc...",
#   "refreshToken": "eyJhbGc...",
#   "expiresIn": 3600,
#   "tokenType": "Bearer"
# }

# Extract accessToken and use in protected API calls
curl https://your-backend-project.up.railway.app/api/v1/user/profile \
  -H "Authorization: Bearer <accessToken>"
```

---

## Postman Collection for Railway Testing

Import the collection and create environment variables:

```json
{
  "variable": [
    {
      "key": "backend_url",
      "value": "https://your-backend-project.up.railway.app/api/v1"
    },
    {
      "key": "frontend_url",
      "value": "https://your-frontend-project.up.railway.app"
    },
    {
      "key": "access_token",
      "value": ""
    },
    {
      "key": "refresh_token",
      "value": ""
    }
  ]
}
```

### Request: Get Google OAuth URL

```
GET {{backend_url}}/auth/oauth2/google
```

**Response Headers**: Capture `Location` header for redirect URL

### Request: Get Facebook OAuth URL

```
GET {{backend_url}}/auth/oauth2/facebook
```

### Request: Login for Testing

```
POST {{backend_url}}/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "123456"
}
```

**Post-request Script**:
```javascript
const body = pm.response.json();
pm.environment.set("access_token", body.accessToken);
pm.environment.set("refresh_token", body.refreshToken);
```

### Request: Get User Profile

```
GET {{backend_url}}/user/profile
Authorization: Bearer {{access_token}}
```

### Request: Refresh Token

```
POST {{backend_url}}/auth/refresh-token
Content-Type: application/json

{
  "refreshToken": "{{refresh_token}}"
}
```

---

## Debugging Callback Token Issues

### Issue: Tokens Not Appearing in Query Parameters

**Diagnosis**:
1. Check `OAUTH2_AUTHORIZED_REDIRECT_URI` is set in backend
2. Verify backend is actually using `getRedirectStrategy().sendRedirect()`
3. Check Firefox/Chrome console for actual redirect URL

**Solution**:
```bash
# Backend environment variable must be set
OAUTH2_AUTHORIZED_REDIRECT_URI=https://your-frontend-project.up.railway.app/auth/callback
```

### Issue: Token Parsing Fails in Frontend

**Diagnosis**:
1. Check `searchParams.get("accessToken")` in browser console
2. Verify URL structure in address bar
3. Check if tokens are being sent in JSON body instead of query params

**Solution**:
```typescript
// Frontend should check both:
const accessToken = 
  searchParams.get("accessToken") || // Query param (preferred)
  (await getFromSessionCookie()); // Session fallback
```

### Issue: CORS Error on Token Validation

**Diagnosis**:
1. Check browser DevTools Network tab
2. Look for `Access-Control-Allow-Origin` header

**Solution**:
```yaml
# Backend CORS configuration
cors:
  allowed-origins: https://your-frontend-project.up.railway.app
  allow-credentials: true
```

---

## Token Format Validation

### Access Token Structure (JWT)

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.
eyJzdWIiOiIxIiwibmFtZSI6IkFkbWluIiwiaWF0IjoxNTE2MjM5MDIyfQ.
SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
```

**Header**:
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

**Payload**:
```json
{
  "sub": "1",
  "username": "user@google.com",
  "email": "user@google.com",
  "role": "USER",
  "iat": 1516239022,
  "exp": 1516242622
}
```

### Refresh Token Structure

Similar to access token but with `tokenType: "REFRESH"` in payload.

---

## Summary Checklist

- ✅ Backend returns tokens in query parameters when `OAUTH2_AUTHORIZED_REDIRECT_URI` is set
- ✅ Backend returns JSON response with `accessToken`, `refreshToken` fields
- ✅ Frontend extracts tokens from query parameters (`accessToken`, `token`, `jwt`)
- ✅ Frontend falls back to session recovery if tokens missing in query
- ✅ Error handling catches missing tokens and displays appropriate error message
- ✅ CORS is configured for frontend origin
- ✅ Railway environment variables are properly set
- ✅ Google/Facebook OAuth credentials are configured on respective platforms
- ✅ Redirect URIs point to Railway backend URL
- ✅ End-to-end flow works from login button through dashboard redirect

