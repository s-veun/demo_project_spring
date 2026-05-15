# 401 Unauthorized - Quick Reference Troubleshooting Guide

## 🚨 Getting 401 Unauthorized? Use This Checklist

### Step 1: Verify Endpoint Path is in permitAll()

**Open**: `src/main/java/com/example/demo_project_spring_boot/config/SecurityConfig.java`

**Search** for line with `permitAll()` that lists endpoints:

```java
// Around line 119-131
.requestMatchers(HttpMethod.POST,
    "/api/v1/register",              // ← Is your endpoint here?
    "/api/v1/login",                 // ← Is your endpoint here?
    "/api/v1/auth/register",
    "/api/v1/auth/login",
    "/api/v1/auth/refresh-token",
    "/api/v1/admin/register",
    "/api/v1/admin/login"
).permitAll()
```

**Action**: If your endpoint is NOT listed, add it to this `permitAll()` block.

---

### Step 2: Verify Request Headers

**Run this curl command**:

```bash
curl -v POST http://localhost:8080/api/v1/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test"}'
```

**Look for these headers in output**:

```
> POST /api/v1/register HTTP/1.1
> Host: localhost:8080
> Content-Type: application/json
< HTTP/1.1 401 Unauthorized           ← PROBLEM HERE
```

**Check**: 
- ✅ Request path is correct
- ✅ HTTP method is correct (POST vs GET)
- ✅ No `Authorization: Bearer` header present (should NOT be there for public endpoints)

---

### Step 3: Check SecurityConfig Building Correctly

**Run**:

```bash
cd /Users/ppc/Desktop/ecommerce_app/demo_project_spring_boot
./gradlew compileJava
```

**Expected**: `BUILD SUCCESSFUL`

**If ERROR**: Fix compilation errors in SecurityConfig

---

### Step 4: Restart Spring Boot Application

Remember to **restart** the application after modifying `SecurityConfig.java`:

```bash
# Kill current process
Ctrl+C

# Restart
./gradlew bootRun
```

---

## 🔍 Detailed Diagnosis

### Is Endpoint in permitAll()?

```bash
# Search SecurityConfig for your endpoint path
grep -n "/api/v1/register" src/main/java/config/SecurityConfig.java

# Should output something like:
# 120: "/api/v1/register",
# 
# If NO output, add the endpoint to permitAll()
```

### Does JWT Filter Skip Missing Headers?

Check `JwtAuthenticationFilter.java`:

```java
final String authHeader = request.getHeader("Authorization");

// If no Authorization header → continue (correct behavior)
if (authHeader == null || authHeader.isBlank()) {
    filterChain.doFilter(request, response);  // ✅ Correct
    return;
}
```

✅ If this code is present, JWT filter is configured correctly.

### Are Swagger & API Docs Accessible?

Test without authentication:

```bash
curl -I http://localhost:8080/swagger-ui.html    # Should return 200
curl -I http://localhost:8080/v3/api-docs        # Should return 200
```

❌ If returns 401, add these in permitAll():
```java
.requestMatchers(
    "/swagger-ui/**",
    "/swagger-ui.html",
    "/v3/api-docs/**",
    "/v3/api-docs"
).permitAll()
```

---

## 🛠️ Common Fixes

### Fix 1: Add Missing Endpoint to permitAll()

```java
// BEFORE (Broken)
.requestMatchers(HttpMethod.POST,
    "/api/v1/auth/register"
).permitAll()

// AFTER (Fixed)
.requestMatchers(HttpMethod.POST,
    "/api/v1/register",        // ← ADDED
    "/api/v1/auth/register"
).permitAll()
```

---

### Fix 2: Add Missing HTTP Method

```java
// BEFORE (only POST)
.requestMatchers(HttpMethod.POST,
    "/api/v1/login"
).permitAll()

// AFTER (added GET if needed)
.requestMatchers(HttpMethod.POST,
    "/api/v1/login"
).permitAll()

.requestMatchers(HttpMethod.GET,
    "/api/v1/auth/oauth2/google"     // ← ADDED for GET
).permitAll()
```

---

### Fix 3: Ensure Proper Exception Handling

```java
// In SecurityConfig.java
.exceptionHandling(ex -> ex
    .authenticationEntryPoint(customAuthenticationEntryPoint)  // 401 handling
    .accessDeniedHandler(customAccessDeniedHandler))          // 403 handling
```

✅ If missing, add this configuration block.

---

### Fix 4: Test with Postman Collection

Import: `postman/401_unauthorized_fix_collection.json`

This collection tests:
- ✅ Public endpoints (should return 200/201)
- ✅ Protected endpoints without token (should return 401)
- ✅ Protected endpoints with token (should return 200)

---

## 📋 Endpoint Checklist

### Public Endpoints (No Authentication)

```
✅ All these should work WITHOUT Authorization header:

POST /api/v1/register
POST /api/v1/login
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh-token
POST /api/v1/admin/register
POST /api/v1/admin/login

GET /api/v1/auth/oauth2/google
GET /api/v1/auth/oauth2/facebook
GET /api/v1/products
GET /api/v1/categories
GET /swagger-ui/**
GET /v3/api-docs/**
GET /actuator/health
```

**If ANY of these return 401**: 
1. Check `SecurityConfig.permitAll()` includes this path
2. Verify HTTP method (POST vs GET)
3. Ensure no Authorization header is sent

---

### Protected Endpoints (Require Authentication)

```
❌ All these should FAIL with 401 WITHOUT Authorization header:

GET /api/v1/user/profile
GET /api/v1/admin/users
POST /api/v1/orders

✅ Should WORK with valid JWT Authorization header:

Authorization: Bearer <access_token_from_login>
```

**If protected endpoint returns 200 without token**: 
1. This is a SECURITY BUG
2. Check if it's accidentally in `permitAll()`
3. Verify `hasRole()` or `hasAnyRole()` is applied

---

## 🔧 Emergency Fixes

### If Public Endpoints Still Getting 401 After Fix

**Step 1**: Verify app restarted
```bash
ps aux | grep java
# Should show java process running

# If not running, restart:
./gradlew bootRun
```

**Step 2**: Enable debug logging
```properties
# application.properties
logging.level.org.springframework.security=DEBUG
logging.level.org.springframework.security.web.access.intercept.FilterSecurityInterceptor=DEBUG
```

**Step 3**: Look at server logs for which endpoint matching occurs

**Step 4**: Try accessing from CLI before web browser
```bash
curl -X POST http://localhost:8080/api/v1/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test","email":"test@test.com"}'
```

---

### If Only Some Paths Return 401

**Examples**:
- `/api/v1/auth/register` works ✅
- `/api/v1/register` returns 401 ❌

**Cause**: Different controller mapped to different path

**Solution**: 
1. Find all `@PostMapping("/register")` in codebase:
   ```bash
   grep -r "@PostMapping.*register" src/
   ```

2. Add EVERY unique path to `permitAll()`:
   ```java
   .requestMatchers(HttpMethod.POST,
       "/api/v1/register",        // UserController
       "/api/v1/auth/register",   // AuthenticationController
       "/api/v1/admin/register"   // AdminController
   ).permitAll()
   ```

---

## 🧪 Complete Test Sequence

1. **Restart app**
   ```bash
   ./gradlew bootRun
   ```

2. **Test public endpoint**
   ```bash
   curl -X POST http://localhost:8080/api/v1/register \
     -H "Content-Type: application/json" \
     -d '{"username":"test","email":"test@test.com","password":"123456"}'
   ```
   Expected: 201 Created OR 200 OK (not 401)

3. **If still 401**: 
   - Check logs for "request matching" output
   - Enable DEBUG logging (see above)
   - Verify SecurityConfig file was saved

4. **Test with valid token**
   ```bash
   # 1. Login first
   curl -X POST http://localhost:8080/api/v1/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"test","password":"123456"}' > token.json
   
   # 2. Extract token
   TOKEN=$(cat token.json | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
   
   # 3. Use token
   curl -X GET http://localhost:8080/api/v1/user/profile \
     -H "Authorization: Bearer $TOKEN"
   ```
   Expected: 200 OK with profile

---

## 📞 Still Having Issues?

1. **Check files exist**:
   ```bash
   ls -la src/main/java/com/example/demo_project_spring_boot/config/SecurityConfig.java
   ls -la src/main/java/com/example/demo_project_spring_boot/security/
   ```

2. **Check build compiles**:
   ```bash
   ./gradlew build --no-daemon 2>&1 | grep -i error
   ```

3. **Check runtime logs**:
   ```bash
   ./gradlew bootRun 2>&1 | grep -i "401\|unauthorized\|permit"
   ```

4. **Review Documentation**:
   - `docs/401_UNAUTHORIZED_FIX.md` (comprehensive guide)
   - `docs/401_DEBUGGING_GUIDE.md` (visual debugging)
   - `docs/IMPLEMENTATION_SUMMARY.md` (complete changes)

---

## 💡 Key Concepts to Remember

| Concept | What It Does | Example |
|---------|-------------|---------|
| **permitAll()** | Endpoint needs NO authentication | `/api/v1/register` |
| **authenticated()** | Endpoint needs valid JWT token | `/api/v1/user/profile` |
| **hasRole("ADMIN")** | Endpoint needs ADMIN role | `/api/v1/admin/users` |
| **@EnableWebSecurity** | Enables Spring Security | On SecurityConfig class |
| **JwtAuthenticationFilter** | Validates JWT in requests | Runs before Spring Authorization |
| **SecurityFilterChain** | Defines authorization rules | Most important configuration |

---

## 🎯 Remember

```
401 Unauthorized = "Authentication Required"
403 Forbidden = "You don't have permission"

If PUBLIC endpoint returns 401:
→ Add to permitAll()

If PROTECTED endpoint returns 401:
→ User needs valid JWT token

If PROTECTED endpoint returns 403:
→ User needs higher role/permission
```

**Need help?** Check the documentation files or enable DEBUG logging to see exact matching errors.

