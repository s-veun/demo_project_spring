# Spring Security 401 Unauthorized - Visual Debugging Guide

## The Problem

User makes a request to `/api/v1/register` and gets:

```
HTTP/1.1 401 Unauthorized

{
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "path": "/api/v1/register"
}
```

## Why Does This Happen?

### The Request Flow (Visual)

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. Browser/Client sends HTTP Request                            │
│    POST /api/v1/register                                        │
│    Content-Type: application/json                               │
│    (No Authorization header)                                    │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. Spring Security FilterChain Processes Request                │
│                                                                 │
│    a) JwtAuthenticationFilter runs                              │
│       └─→ Looks for "Authorization" header                      │
│           - Header NOT found → go to next filter                │
│           - No exception thrown ✓                               │
│                                                                 │
│    b) SecurityFilterChain.authorizeHttpRequests() runs          │
│       └─→ Checks: Is this endpoint in permitAll()?              │
│           - OLD (BROKEN): /api/v1/register NOT in list          │
│             └─→ Reject request → 401 ✗                         │
│           - NEW (FIXED): /api/v1/register IS in list            │
│             └─→ Allow request → Load controller ✓              │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3. AuthenticationController.register() executes                 │
│    └─→ Creates user                                             │
│    └─→ Returns 201 Created ✓                                    │
└─────────────────────────────────────────────────────────────────┘
```

## The Fix - Four Key Components

### ✅ Fix #1: JwtAuthenticationFilter (Already Correct)

**File**: `src/main/java/config/JwtAuthenticationFilter.java`

```java
@Override
protected void doFilterInternal(HttpServletRequest request, 
                               HttpServletResponse response,
                               FilterChain filterChain) 
        throws ServletException, IOException {
    
    final String authHeader = request.getHeader("Authorization");
    
    // KEY: If NO Authorization header → don't validate JWT, continue
    if (authHeader == null || authHeader.isBlank()) {
        filterChain.doFilter(request, response);  // ✅ Continues to next filter
        return;
    }
    
    // If Authorization header exists → validate token
    if (!authHeader.startsWith("Bearer ")) {
        filterChain.doFilter(request, response);
        return;
    }
    
    // Extract and validate JWT...
}
```

**Why this is correct**: 
- Does NOT throw exception when Authorization header is missing
- Lets request continue to the next filter (SecurityConfig)
- Only validates JWT if the header is present

---

### ✅ Fix #2: SecurityConfig permitAll() Configuration (FIXED)

**File**: `src/main/java/config/SecurityConfig.java`

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
        
            // ❌ OLD (BROKEN) - Missing /api/v1/register
            .requestMatchers(HttpMethod.POST,
                "/api/v1/auth/register",      // ✓ has this
                "/api/v1/auth/login"           // ✓ has this
            ).permitAll()
            
            // ✅ NEW (FIXED) - Added all register/login paths
            .requestMatchers(HttpMethod.POST,
                "/api/v1/register",            // ✅ ADDED
                "/api/v1/login",               // ✅ ADDED
                "/api/v1/auth/register",
                "/api/v1/auth/login",
                "/api/v1/auth/refresh-token",
                "/api/v1/admin/register",
                "/api/v1/admin/login"
            ).permitAll()
            
            // OAuth2 endpoints
            .requestMatchers(HttpMethod.GET,
                "/api/v1/auth/oauth2/google",
                "/api/v1/auth/oauth2/facebook"
            ).permitAll()
            
            // Protected endpoints
            .requestMatchers("/api/v1/user/**").hasAnyRole("USER", "ADMIN")
            .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
            
            // Default: require authentication
            .anyRequest().authenticated()
        );
    
    return http.build();
}
```

**Why this is the fix**:
- Explicitly lists `/api/v1/register` and `/api/v1/login` in `permitAll()`
- Spring Security now knows these endpoints don't require authentication
- Matches ALL possible paths where register/login might be defined

---

### ✅ Fix #3: Authentication Entry Point (Error Handling)

**File**: `src/main/java/security/CustomAuthenticationEntryPoint.java`

```java
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                        AuthenticationException authException) 
            throws IOException {
        
        // Return JSON error instead of redirecting
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        Map<String, Object> error = Map.of(
            "status", 401,
            "error", "Unauthorized",
            "message", "Full authentication is required",
            "path", request.getServletPath()
        );
        
        response.getWriter().write(new ObjectMapper().writeValueAsString(error));
    }
}
```

**Why this matters**:
- Returns JSON error instead of HTML redirect
- Appropriate for REST APIs (not web forms)
- Helps debugging by showing which endpoint was rejected

---

### ✅ Fix #4: CORS Configuration

**File**: `src/main/java/config/SecurityConfig.java`

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    
    // Allow frontend domains
    config.setAllowedOriginPatterns(List.of("http://localhost:3000", "http://localhost:3001"));
    
    // Allow all HTTP methods
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    
    // Allow all headers
    config.setAllowedHeaders(List.of("*"));
    
    // Expose Authorization header
    config.setExposedHeaders(List.of("Authorization", "Content-Type"));
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
```

---

## Debugging Checklist

### ✅ If /register returns 401:

Check in order:

1. **Verify controller path**
   ```bash
   grep -r "@PostMapping.*register" src/main/java/
   # Should output multiple lines with different paths
   ```

2. **Verify all paths in permitAll()**
   ```java
   // SecurityConfig.java line ~119
   .requestMatchers(HttpMethod.POST,
       "/api/v1/register",      // ← Must be here
       "/api/v1/auth/register"  // ← Must be here
   ).permitAll()
   ```

3. **Check request headers**
   ```bash
   curl -v POST http://localhost:8080/api/v1/register \
     -H "Content-Type: application/json" \
     -d '{"username":"test"}'
   # Check: No "Authorization:" header in request
   ```

4. **Enable debug logging**
   ```properties
   # application.yml
   logging:
     level:
       org.springframework.security: DEBUG
       org.springframework.security.web.access.intercept.FilterSecurityInterceptor: DEBUG
   ```

### ✅ If /profile returns 401 (should happen without token):

This is CORRECT behavior. Verify:

1. **No Authorization header sent**
   ```bash
   curl http://localhost:8080/api/v1/user/profile
   # ✓ Should return 401
   ```

2. **With Authorization header**
   ```bash
   curl http://localhost:8080/api/v1/user/profile \
     -H "Authorization: Bearer <token_from_login>"
   # ✓ Should return 200 with profile
   ```

---

## Common Mistakes

| Mistake | Result | Fix |
|---------|--------|-----|
| Forget to add `/api/v1/register` to permitAll() | 401 on register | Add path to permitAll() list |
| JWT Filter throws exception on missing header | Breaks all public endpoints | Ensure filter continues chain when header missing |
| Require authentication globally | Everything returns 401 | Use `.anyRequest().authenticated()` at END |
| Wrong HTTP method matcher | Only specific method blocked | Ensure `HttpMethod.POST` matches actual request |
| CORS blocking requests | Browser blocks response | Configure CORS properly |
| Swagger not accessible | 401 when accessing /swagger-ui | Add to permitAll() |

---

## Testing Sequence

```
1. START HERE → POST /api/v1/register (public, no token)
                ↓
2. Should return 201 Created
                ↓
3. If returns 401 → Check permitAll() configuration
                ↓
4. Login → POST /api/v1/auth/login
                ↓
5. Copy accessToken from response
                ↓
6. GET /api/v1/user/profile with token
                ↓
7. Should return 200 with profile data
                ↓
8. POST /api/v1/auth/logout with token
                ↓
9. Try GET /api/v1/user/profile again with same token
                ↓
10. Should return 401 (token revoked)
```

---

## Postman Test Sequence

```json
1. Register User
   POST {{baseUrl}}/api/v1/register
   Body: { "username": "test", "email": "test@example.com", "password": "123456" }
   Expected: 201 Created

2. Login
   POST {{baseUrl}}/api/v1/auth/login
   Body: { "username": "test", "password": "123456" }
   Expected: 200 OK with accessToken
   
3. Copy accessToken into {{accessToken}} variable

4. Get Profile
   GET {{baseUrl}}/api/v1/user/profile
   Header: Authorization: Bearer {{accessToken}}
   Expected: 200 OK with user data

5. Logout
   POST {{baseUrl}}/api/v1/auth/logout
   Header: Authorization: Bearer {{accessToken}}
   Expected: 200 OK

6. Try Profile After Logout
   GET {{baseUrl}}/api/v1/user/profile
   Header: Authorization: Bearer {{accessToken}}
   Expected: 401 Unauthorized (token revoked)
```

---

## Key Takeaways

| Concept | Explanation |
|---------|-------------|
| **permitAll()** | Endpoints that don't require authentication |
| **hasRole()** | Endpoints that require specific role |
| **authenticated()** | Endpoints that require valid JWT token |
| **JwtAuthenticationFilter** | Validates JWT tokens IF Authorization header present |
| **SecurityFilterChain** | Decides which endpoints are public vs protected |
| **@EnableWebSecurity** | Enables Spring Security configuration |
| **@EnableMethodSecurity** | Enables @PreAuthorize/@PostAuthorize annotations |
| **CSRF disabled** | Required for REST APIs (no stateful sessions) |
| **Stateless sessions** | No server-side sessions, all state in JWT |

---

## Production Security Checklist

Before deploying to production:

- [ ] Change CORS origins from `*` to specific domain
- [ ] Change JWT secret to cryptographically secure random value
- [ ] Enable HTTPS everywhere
- [ ] Set appropriate token expiration times (short for access, longer for refresh)
- [ ] Enable request rate limiting
- [ ] Configure logging/audit trail
- [ ] Use environment variables for secrets (NOT in code)
- [ ] Test with multiple browsers and tools
- [ ] Monitor security logs for suspicious activity
- [ ] Regularly update Spring Security dependencies

