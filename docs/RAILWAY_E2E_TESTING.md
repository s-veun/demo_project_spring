# OAuth2 End-to-End Testing with Railway

Complete testing guide for validating Google/Facebook OAuth2 integration with Railway deployment.

## Prerequisites

- Backend deployed on Railway (e.g., https://your-backend.up.railway.app)
- Frontend deployed on Railway (e.g., https://your-frontend.up.railway.app)
- Valid Google OAuth2 credentials configured
- Valid Facebook App credentials configured
- All environment variables properly set

---

## Railway Environment Setup

### Backend Environment Variables

Set these on your Railway backend service:

```bash
# Database
SPRING_DATASOURCE_URL=postgresql://user:password@hostname:port/dbname
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=password

# JWT
JWT_SECRET=your-256-bit-base64-encoded-secret
JWT_ACCESS_TOKEN_EXPIRATION=3600000
JWT_REFRESH_TOKEN_EXPIRATION=2592000000

# Google OAuth2
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
GOOGLE_REDIRECT_URI=https://your-backend.up.railway.app/login/oauth2/code/google

# Facebook OAuth2
FACEBOOK_APP_ID=your-facebook-app-id
FACEBOOK_APP_SECRET=your-facebook-app-secret
FACEBOOK_REDIRECT_URI=https://your-backend.up.railway.app/login/oauth2/code/facebook

# Frontend Callback
OAUTH2_AUTHORIZED_REDIRECT_URI=https://your-frontend.up.railway.app/auth/callback

# CORS
CORS_ALLOWED_ORIGINS=https://your-frontend.up.railway.app
```

### Frontend Environment Variables

Set on your Railway frontend service:

```bash
NEXT_PUBLIC_API_BASE_URL=https://your-backend.up.railway.app/api/v1
NEXT_PUBLIC_AUTH_LOGIN_PATH=/auth/login
NEXT_PUBLIC_AUTH_REGISTER_PATH=/auth/register
NEXT_PUBLIC_AUTH_REFRESH_PATH=/auth/refresh-token
NEXT_PUBLIC_AUTH_LOGOUT_PATH=/auth/logout
NEXT_PUBLIC_OAUTH_GOOGLE_PATH=/oauth2/authorization/google
NEXT_PUBLIC_OAUTH_FACEBOOK_PATH=/oauth2/authorization/facebook
```

---

## Test Case 1: Google OAuth2 Complete Flow

### Step 1: Start at Login Page

```bash
# Navigate to
https://your-frontend.up.railway.app/login

# Expected: Login page displays with "Continue with Google" button
```

### Step 2: Click "Continue with Google"

```bash
# Frontend calls
https://your-backend.up.railway.app/api/v1/oauth2/authorization/google

# Backend returns redirect to Google authorization endpoint
# Browser is redirected to
https://accounts.google.com/o/oauth2/v2/auth?client_id=...&redirect_uri=...&scope=...
```

### Step 3: Authenticate with Google

```bash
# User logs in with Google account
# Google shows consent screen
# User clicks "Allow"
```

### Step 4: Google Redirects Back to Backend

```bash
# Google redirects to
https://your-backend.up.railway.app/login/oauth2/code/google?code=...&state=...

# Backend processes OAuth code:
# 1. Exchanges code for Google access token
# 2. Fetches user info from Google
# 3. Creates or updates user in database
# 4. Generates JWT access and refresh tokens
```

### Step 5: Backend Redirects to Frontend Callback

```bash
# Backend redirects to configured callback URI with tokens
https://your-frontend.up.railway.app/auth/callback?accessToken=eyJhbGc...&refreshToken=eyJhbGc...&provider=GOOGLE

# Expected Status: 302 Redirect
```

### Step 6: Frontend Parses Tokens and Redirects to Dashboard

```bash
# OAuthCallbackClient extracts tokens from URL query parameters
accessToken = searchParams.get("accessToken")  // eyJhbGc...
refreshToken = searchParams.get("refreshToken") // eyJhbGc...
provider = searchParams.get("provider")         // GOOGLE

# Frontend stores tokens in localStorage
# Frontend redirects to /dashboard
```

### Step 7: Verify Dashboard Access

```bash
# Frontend loads dashboard page
https://your-frontend.up.railway.app/dashboard

# Expected: User profile displayed with authentication
# Make API call to verify token works
curl https://your-backend.up.railway.app/api/v1/user/profile \
  -H "Authorization: Bearer eyJhbGc..."

# Expected Response: 200 OK with user profile
```

---

## Test Case 2: Facebook OAuth2 Complete Flow

### Follow same steps as Google, but:

```bash
# Step 2: Click "Continue with Facebook"
# Backend returns
https://www.facebook.com/v18.0/dialog/oauth?client_id=...&redirect_uri=...

# Step 4: Facebook redirects to
https://your-backend.up.railway.app/login/oauth2/code/facebook?code=...&state=...

# Steps 5-7 are identical to Google flow
```

---

## Test Case 3: Error Handling - User Denies Access

### Step 1: Start OAuth Flow

```bash
https://your-frontend.up.railway.app/login
# Click "Continue with Google"
```

### Step 2: User Denies Access

```bash
# At Google consent screen, user clicks "Cancel"
# Google redirects to backend with error
https://your-backend.up.railway.app/login/oauth2/code/google?error=access_denied
```

### Step 3: Backend Handles Error

```bash
# Backend detects error and redirects to frontend callback with error params
https://your-frontend.up.railway.app/auth/callback?error=oauth2_authentication_failed&message=User+denied+access

# Or returns JSON error if no callback URI configured:
# {
#   "success": false,
#   "status": 401,
#   "error": "Authentication Failed",
#   "message": "OAuth2 authentication failed"
# }
```

### Step 4: Frontend Displays Error

```bash
# OAuthCallbackClient detects error parameter
error = searchParams.get("error")  // "oauth2_authentication_failed"
message = searchParams.get("message")  // "User denied access"

# Shows error toast notification
showToast({ 
  type: "error", 
  title: "OAuth login failed", 
  message: message 
})

# Redirects back to login page
router.replace("/login")
```

---

## Test Case 4: Token Refresh on Expired Access Token

### Step 1: Make Protected API Call with Expired Token

```bash
# Simulate expired access token scenario
curl https://your-backend.up.railway.app/api/v1/user/profile \
  -H "Authorization: Bearer expired_token_xyz"

# Expected: 401 Unauthorized response
```

### Step 2: Frontend Detects 401 and Calls Refresh

```bash
# Frontend calls refresh endpoint with refresh token
curl -X POST https://your-backend.up.railway.app/api/v1/auth/refresh-token \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"eyJhbGc_refresh..."}'

# Expected Response:
# {
#   "success": true,
#   "accessToken": "eyJhbGc_new_token...",
#   "refreshToken": "eyJhbGc_new_refresh...",
#   "expiresIn": 3600,
#   "tokenType": "Bearer"
# }
```

### Step 3: Frontend Retries Original Request

```bash
# Frontend stores new access token
# Frontend retries original API call
curl https://your-backend.up.railway.app/api/v1/user/profile \
  -H "Authorization: Bearer eyJhbGc_new_token..."

# Expected: 200 OK with user profile
```

---

## Test Case 5: Logout and Token Revocation

### Step 1: User Clicks Logout

```bash
# Frontend should call logout endpoint
curl -X POST https://your-backend.up.railway.app/api/v1/auth/logout \
  -H "Authorization: Bearer eyJhbGc_access_token..." \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"eyJhbGc_refresh_token..."}'

# Expected: 200 OK
```

### Step 2: Tokens Are Revoked

```bash
# Frontend clears tokens from localStorage
localStorage.removeItem('accessToken')
localStorage.removeItem('refreshToken')

# Frontend redirects to login page
```

### Step 3: Verify Tokens Work No More

```bash
# Frontend tries to access protected endpoint
curl https://your-backend.up.railway.app/api/v1/user/profile \
  -H "Authorization: Bearer eyJhbGc_revoked_token..."

# Expected: 401 Unauthorized response
```

---

## Automated Testing Script

### Run Auth Smoke Test

```bash
# Backend
cd /Users/ppc/Desktop/ecommerce_app/demo_project_spring_boot

# Frontend
cd /Users/ppc/Desktop/ecommerce_app/table_eco_table_frontend

# Run test
AUTH_SMOKE_USERNAME=admin AUTH_SMOKE_PASSWORD=123456 npm run auth:smoke
```

---

## Browser Console Debugging

### Test Token Extraction in Browser

```javascript
// Open browser DevTools Console on callback page
// Extract token from current URL
const url = new URL(window.location.href);
const accessToken = url.searchParams.get("accessToken");
const refreshToken = url.searchParams.get("refreshToken");
const provider = url.searchParams.get("provider");

console.log("Access Token:", accessToken);
console.log("Refresh Token:", refreshToken);
console.log("Provider:", provider);

// Verify tokens are stored
console.log("Stored Access Token:", localStorage.getItem("accessToken"));
console.log("Stored Refresh Token:", localStorage.getItem("refreshToken"));
```

### Monitor Network Requests

1. Open DevTools > Network tab
2. Perform OAuth login
3. Look for:
   - `GET /login/oauth2/code/google` (backend receives code)
   - Redirect to `/auth/callback?accessToken=...` (backend sends tokens)
   - Subsequent API calls with `Authorization: Bearer` header

---

## CURL Testing for Railway URLs

### Test 1: Check Backend Health

```bash
curl https://your-backend.up.railway.app/actuator/health
# Expected: 200 OK with {"status":"UP"}
```

### Test 2: Check CORS Configuration

```bash
curl -X OPTIONS https://your-backend.up.railway.app/api/v1/user/profile \
  -H "Origin: https://your-frontend.up.railway.app" \
  -H "Access-Control-Request-Method: GET" \
  -v

# Expected headers:
# Access-Control-Allow-Origin: https://your-frontend.up.railway.app
# Access-Control-Allow-Credentials: true
```

### Test 3: Test Redirect URI Resolution

```bash
# Check that backend can reach the callback URL
curl -I https://your-frontend.up.railway.app/auth/callback
# Expected: 200 or 405 (method not allowed) - but reachable
```

---

## Postman Collection

Import this as a new Postman environment for Railway testing:

```json
{
  "name": "OAuth2 Railway Testing",
  "values": [
    {
      "key": "backend_url",
      "value": "https://your-backend.up.railway.app",
      "enabled": true,
      "type": "default"
    },
    {
      "key": "frontend_url",
      "value": "https://your-frontend.up.railway.app",
      "enabled": true,
      "type": "default"
    },
    {
      "key": "api_base_url",
      "value": "{{backend_url}}/api/v1",
      "enabled": true,
      "type": "default"
    },
    {
      "key": "access_token",
      "value": "",
      "enabled": true,
      "type": "default"
    },
    {
      "key": "refresh_token",
      "value": "",
      "enabled": true,
      "type": "default"
    },
    {
      "key": "google_client_id",
      "value": "",
      "enabled": true,
      "type": "default"
    },
    {
      "key": "facebook_app_id",
      "value": "",
      "enabled": true,
      "type": "default"
    }
  ]
}
```

### Requests to Add

```
1. GET {{api_base_url}}/auth/oauth2/google
   Pre-request script: Prepare state parameter
   Test: Verify redirect location header

2. GET {{api_base_url}}/auth/oauth2/facebook
   Pre-request script: Prepare state parameter
   Test: Verify redirect location header

3. POST {{api_base_url}}/auth/login
   Body: {"username":"admin","password":"123456"}
   Post-request script: 
     pm.environment.set("access_token", pm.response.json().accessToken)
     pm.environment.set("refresh_token", pm.response.json().refreshToken)
   Test: Verify 200 OK and tokens present

4. GET {{api_base_url}}/user/profile
   Headers: Authorization: Bearer {{access_token}}
   Test: Verify 200 OK with user profile

5. POST {{api_base_url}}/auth/refresh-token
   Body: {"refreshToken":"{{refresh_token}}"}
   Post-request script:
     pm.environment.set("access_token", pm.response.json().accessToken)
   Test: Verify new access token

6. POST {{api_base_url}}/auth/logout
   Headers: Authorization: Bearer {{access_token}}
   Body: {"refreshToken":"{{refresh_token}}"}
   Test: Verify 200 OK
```

---

## Common Issues and Solutions

### Issue: CORS Error on OAuth Callback

**Symptom**: Error "Access to XMLHttpRequest blocked by CORS policy"

**Solution**:
```yaml
# Backend application.yml
cors:
  allowed-origins: https://your-frontend.up.railway.app
  allow-credentials: true
```

### Issue: Tokens Not Appearing in Callback URL

**Symptom**: Redirect to `https://your-frontend.up.railway.app/auth/callback` without query parameters

**Solution**: Ensure `OAUTH2_AUTHORIZED_REDIRECT_URI` is set in Railway backend environment

```bash
OAUTH2_AUTHORIZED_REDIRECT_URI=https://your-frontend.up.railway.app/auth/callback
```

### Issue: Token Validation Fails

**Symptom**: 401 responses even with valid token

**Solution**: Verify JWT secret matches between backend instances
```bash
# Use same JWT_SECRET in all backend instances
JWT_SECRET=your-consistent-256-bit-secret
```

### Issue: Google/Facebook Redirect URI Mismatch

**Symptom**: "Redirect URI mismatch" error from OAuth provider

**Solution**: Ensure registered redirect URIs match configured values
```bash
# Google Cloud Console
Authorized redirect URIs: https://your-backend.up.railway.app/login/oauth2/code/google

# Facebook App Settings
Valid OAuth Redirect URIs: https://your-backend.up.railway.app/login/oauth2/code/facebook
```

---

## Verification Checklist

- ✅ Backend deployed on Railway with all env vars set
- ✅ Frontend deployed on Railway with all env vars set  
- ✅ Google OAuth credentials configured in Google Cloud Console
- ✅ Facebook OAuth credentials configured in Facebook App Settings
- ✅ Redirect URIs registered on OAuth provider platforms
- ✅ CORS configured on backend for frontend origin
- ✅ HTTPS used for all Railway URLs
- ✅ Database migration applied and tables created
- ✅ JWT secret configured and consistent
- ✅ Callback token parsing validated in browser console
- ✅ Protected API endpoints return 401 without valid token
- ✅ Token refresh flow works correctly
- ✅ Logout revokes tokens
- ✅ Error handling displays proper error messages
- ✅ User profile loaded after successful OAuth
- ✅ Both Google and Facebook flows complete end-to-end

---

## Performance Testing

### Test API Response Time

```bash
# Test without token (should be fast)
time curl https://your-backend.up.railway.app/actuator/health

# Test protected endpoint (includes JWT validation)
time curl https://your-backend.up.railway.app/api/v1/user/profile \
  -H "Authorization: Bearer eyJhbGc..."
```

### Expected Response Times

- Public endpoints: < 100ms
- Protected endpoints: < 150ms (includes JWT validation)
- OAuth redirect: < 500ms
- Token refresh: < 200ms

---

## Load Testing with Railway

### Monitor Railway Metrics

1. Open your Railway project dashboard
2. Check Memory, CPU, Network metrics during OAuth flow
3. Monitor logs for errors or warnings

### Test with Multiple Users

```bash
# Run multiple OAuth flows simultaneously to test concurrency
for i in {1..5}; do
  curl -I https://your-backend.up.railway.app/actuator/health &
done
```

---

## Final Validation

Once all tests pass:

1. ✅ Document the deployed Railway backend URL
2. ✅ Document the deployed Railway frontend URL
3. ✅ Create runbooks for common troubleshooting
4. ✅ Set up monitoring alerts for 401/403 errors
5. ✅ Schedule periodic OAuth flow tests
6. ✅ Document fallback procedures for OAuth provider outages

