# Complete cURL Examples - 401 Unauthorized Fix & JWT Authentication

## 🚀 Quick Start - Copy & Paste Commands

### 1. Start the Application

```bash
cd /Users/ppc/Desktop/ecommerce_app/demo_project_spring_boot
./gradlew bootRun
```

Wait for: `Started DemoProjectSpringBootApplication in X seconds`

---

## ✅ TEST PUBLIC ENDPOINTS (No JWT Required)

### Test 1: Register New User

```bash
curl -X POST http://localhost:8080/api/v1/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "testuser@example.com",
    "password": "password123",
    "firstName": "Test",
    "lastName": "User"
  }'
```

**Expected Response (201 Created)**:
```json
{
  "success": true,
  "message": "User registered successfully",
  "userId": 1,
  "username": "testuser",
  "email": "testuser@example.com",
  "role": "USER"
}
```

**❌ If you get 401 Unauthorized**:
- This means SecurityConfig fix didn't work
- Check: `docs/QUICK_REFERENCE.md` troubleshooting section

---

### Test 2: Login User

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

**Expected Response (200 OK)**:
```json
{
  "success": true,
  "message": "Login successful",
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImVtYWlsIjoidGVzdHVzZXJAZXhhbXBsZS5jb20iLCJ1c2VySWQiOjEsInJvbGVzIjoiVVNFUiIsInR5cGUiOiJBQ0NFU1MiLCJpYXQiOjE3MTU4NDMyMzgsImV4cCI6MTcxNTkyNzYzOH0.signature",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0dXNlciIsInVzZXJJZCI6MSwicm9sZXMiOiJSRUZSRVNIIiwiaWF0IjoxNzE1ODQzMjM4LCJleHAiOjE3MTg0MzUyMzh9.signature",
  "tokenType": "Bearer",
  "userId": 1,
  "username": "testuser",
  "email": "testuser@example.com",
  "firstName": "Test",
  "lastName": "User",
  "role": "USER",
  "expiresIn": 86400
}
```

**⚠️ Important**: Save these token values for next tests

---

### Test 3: Check Swagger UI Accessible

```bash
curl -I http://localhost:8080/swagger-ui.html
```

**Expected**:
```
HTTP/1.1 200 OK
Content-Type: text/html
```

---

### Test 4: Google OAuth2 Info Endpoint

```bash
curl -X GET http://localhost:8080/api/v1/auth/oauth2/google
```

**Expected Response (200 OK)**:
```json
{
  "success": true,
  "message": "Continue with Google",
  "data": {
    "authorizationUri": "/oauth2/authorization/google"
  }
}
```

---

### Test 5: Facebook OAuth2 Info Endpoint

```bash
curl -X GET http://localhost:8080/api/v1/auth/oauth2/facebook
```

**Expected Response (200 OK)**:
```json
{
  "success": true,
  "message": "Continue with Facebook",
  "data": {
    "authorizationUri": "/oauth2/authorization/facebook"
  }
}
```

---

## ❌ TEST PROTECTED ENDPOINTS (JWT Required)

### Test 6: Get Profile WITHOUT Token (Should Fail 401)

```bash
curl -X GET http://localhost:8080/api/v1/user/profile
```

**Expected Response (401 Unauthorized)**:
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "path": "/api/v1/user/profile",
  "timestamp": 1715843238000
}
```

✅ **This is correct behavior** - endpoint is protected.

---

### Test 7: Get Profile WITH Token (Should Work 200)

**Step 1**: Save access token from login response:

```bash
# Extract token from login response and save it
ACCESS_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ..."
```

**Step 2**: Use token in request:

```bash
curl -X GET http://localhost:8080/api/v1/user/profile \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

**Expected Response (200 OK)**:
```json
{
  "success": true,
  "message": "Profile fetched successfully",
  "data": {
    "id": 1,
    "username": "testuser",
    "email": "testuser@example.com",
    "firstName": "Test",
    "lastName": "User",
    "profileImage": null,
    "provider": "LOCAL",
    "role": "USER"
  }
}
```

---

### Test 8: Refresh Access Token

```bash
REFRESH_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ..."

curl -X POST http://localhost:8080/api/v1/auth/refresh-token \
  -H "Content-Type: application/json" \
  -d "{
    \"refreshToken\": \"$REFRESH_TOKEN\"
  }"
```

**Expected Response (200 OK)**:
```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.NEW_TOKEN...",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

---

### Test 9: Logout User

```bash
ACCESS_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
REFRESH_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d "{
    \"refreshToken\": \"$REFRESH_TOKEN\"
  }"
```

**Expected Response (200 OK)**:
```json
{
  "success": true,
  "message": "Logout successful"
}
```

---

### Test 10: Try Protected Endpoint After Logout (Should Fail)

```bash
curl -X GET http://localhost:8080/api/v1/user/profile \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

**Expected Response (401 Unauthorized)**:
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource"
}
```

✅ **This is correct** - token was revoked.

---

## 🧪 Batch Test Script

Save as: `test-auth.sh`

```bash
#!/bin/bash

BASE_URL="http://localhost:8080"

echo "======================"
echo "1. Register User"
echo "======================"
REGISTER_RESPONSE=$(curl -s -X POST $BASE_URL/api/v1/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "batchtest",
    "email": "batchtest@example.com",
    "password": "password123",
    "firstName": "Batch",
    "lastName": "Test"
  }')

echo $REGISTER_RESPONSE | jq .

echo ""
echo "======================"
echo "2. Login"
echo "======================"
LOGIN_RESPONSE=$(curl -s -X POST $BASE_URL/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "batchtest",
    "password": "password123"
  }')

echo $LOGIN_RESPONSE | jq .

ACCESS_TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.accessToken')
REFRESH_TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.refreshToken')

echo ""
echo "======================"
echo "3. Get Profile (Protected)"
echo "======================"
curl -s -X GET $BASE_URL/api/v1/user/profile \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq .

echo ""
echo "======================"
echo "4. Refresh Token"
echo "======================"
curl -s -X POST $BASE_URL/api/v1/auth/refresh-token \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\": \"$REFRESH_TOKEN\"}" | jq .

echo ""
echo "======================"
echo "5. Logout"
echo "======================"
curl -s -X POST $BASE_URL/api/v1/auth/logout \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d "{\"refreshToken\": \"$REFRESH_TOKEN\"}" | jq .

echo ""
echo "======================"
echo "6. Try Profile After Logout (Should Fail)"
echo "======================"
curl -s -X GET $BASE_URL/api/v1/user/profile \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq .
```

**Run**:
```bash
chmod +x test-auth.sh
./test-auth.sh
```

---

## 🔍 Debugging Commands

### Check if Server is Running

```bash
curl -s http://localhost:8080/actuator/health | jq .
```

**Expected**: `"status": "UP"`

---

### Check Request Headers

```bash
# See all request headers
curl -v http://localhost:8080/api/v1/register \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test","email":"test@test.com"}' 2>&1 | grep -E '^>'
```

**Expected output**: Shows request headers (no "Authorization" for public endpoints)

---

### Check Response Headers

```bash
# See response headers
curl -i http://localhost:8080/api/v1/user/profile
```

**Expected**: 
```
HTTP/1.1 401 Unauthorized
Content-Type: application/json
```

---

### View Server Logs for Debugging

```bash
# Terminal 2 - watch logs while testing
./gradlew bootRun 2>&1 | grep -E "Spring|Started|request|Filter|Security"
```

---

### Extract Token from Response

```bash
# Get token and save to file
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123"}' | \
  jq -r '.accessToken' > token.txt

# Use token
token=$(cat token.txt)
curl -H "Authorization: Bearer $token" http://localhost:8080/api/v1/user/profile
```

---

## 📊 Expected Status Codes

| Endpoint | No Token | Valid Token | Invalid Token | Reason |
|----------|----------|-------------|---------------|--------|
| `/api/v1/register` | ✅ 201 | - | - | Public endpoint |
| `/api/v1/auth/login` | ✅ 200 | - | - | Public endpoint |
| `/api/v1/user/profile` | ❌ 401 | ✅ 200 | ❌ 401 | Protected endpoint |
| `/api/v1/auth/logout` | ❌ 401 | ✅ 200 | ❌ 401 | Protected endpoint |
| `/swagger-ui.html` | ✅ 200 | - | - | Public documentation |

---

## 🛡️ Security Headers to Check

### Good Response Example (Protected Endpoint with Valid Token)

```bash
curl -i -X GET http://localhost:8080/api/v1/user/profile \
  -H "Authorization: Bearer <valid_token>"
```

**Should see**:
```
HTTP/1.1 200 OK
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
Strict-Transport-Security: max-age=31536000 ; includeSubDomains
Content-Type: application/json
```

---

## 🚀 Performance Tips

### Parallel Testing (if you want to stress test)

```bash
# Test 10 concurrent requests
for i in {1..10}; do
  curl -s http://localhost:8080/api/v1/products &
done
wait
```

---

### Watch Request/Response Times

```bash
curl -w "Total: %{time_total}s, Connect: %{time_connect}s\n" \
  http://localhost:8080/api/v1/products
```

---

## ✔️ Validation Checklist

After running all tests, verify:

- ✅ Register returns 201 (public endpoint works)
- ✅ Login returns 200 with tokens (public endpoint works)
- ✅ Profile WITHOUT token returns 401 (protection works)
- ✅ Profile WITH token returns 200 (authentication works)
- ✅ Swagger UI returns 200 (public docs work)
- ✅ After logout, token doesn't work (revocation works)

---

## 🎯 What Each Test Proves

| Test | Proves |
|------|--------|
| Register public endpoint | SecurityConfig permitAll() working |
| Login public endpoint | Public auth endpoints accessible |
| Profile without token = 401 | Protection is enforced |
| Profile with token = 200 | JWT validation working |
| Swagger accessible | Documentation endpoints public |
| After logout fails | Token revocation working |

---

## 💡 Common Response Patterns

### Success Response (2xx)

```json
{
  "success": true,
  "message": "Operation successful",
  "data": { ... }
}
```

### Unauthorized Response (401)

```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required",
  "path": "/api/v1/endpoint"
}
```

### Forbidden Response (403)

```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "You do not have permission to access this resource",
  "path": "/api/v1/admin/something"
}
```

### Server Error (5xx)

```json
{
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred",
  "timestamp": 1715843238000
}
```

---

## 🔗 Next Steps

1. **Run all tests** and verify expected responses
2. **Check documentation** if any test fails:
   - `docs/401_UNAUTHORIZED_FIX.md` 
   - `docs/QUICK_REFERENCE.md`
3. **Enable DEBUG logging** if still having issues
4. **Contact support** with exact error response

---

## 📖 Additional Resources

- `docs/IMPLEMENTATION_SUMMARY.md` - Complete implementation details
- `docs/401_DEBUGGING_GUIDE.md` - Visual debugging guide
- `postman/401_unauthorized_fix_collection.json` - Postman collection
- `AUTH_MODULE_README.md` - OAuth2 module documentation

---

**All tests passing? 🎉 Your 401 Unauthorized issue is fixed!**

