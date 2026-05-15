# 401 Unauthorized Error - Complete Guide & Fix

## Problem Description

When attempting to access `/api/v1/register` or `/api/v1/login`, the API returns:

```json
{
  "path": "/api/v1/register",
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "status": 401
}
```

## Root Cause Analysis

### Why This Happens

Spring Security denies access to endpoints when:

1. **Endpoint not in `permitAll()`**: The security filter chain requires authentication for all requests by default
2. **Multiple conflicting endpoint patterns**: Different controllers define `/register` at different paths
3. **Missing Authorization header**: REST endpoints require explicit `permitAll()` configuration
4. **Filter chain ordering**: Filters execute in a specific order, and misconfiguration can block requests early

### Spring Security Authorization Flow

```
HTTP Request
    ↓
JwtAuthenticationFilter (custom)
    ↓ (no Authorization header → skip authentication)
SecurityFilterChain.authorizeHttpRequests()
    ↓ (check if endpoint is in permitAll())
    ├─ If permitAll() → allow access ✓
    └─ If not → require authentication ✗ → return 401
```

### The Exact Issue

Our application had **3 different register endpoints** at different paths:

| Endpoint | Controller | Status |
|----------|-----------|--------|
| `/api/v1/register` | UserController | ❌ NOT in permitAll() |
| `/api/v1/auth/register` | AuthenticationController | ✓ in permitAll() |
| `/api/v1/admin/register` | AdminController | ✓ in permitAll() |

When requests went to `/api/v1/register`, Spring Security couldn't find it in the `permitAll()` list and rejected with **401 Unauthorized**.

## Step-by-Step Fix

### Step 1: Identify All Public Endpoints

List all endpoints that should be accessible **without authentication**:

```java
// Authentication Endpoints
POST /api/v1/register
POST /api/v1/login
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh-token
POST /api/v1/admin/register
POST /api/v1/admin/login

// OAuth2 Endpoints
GET /api/v1/auth/oauth2/google
GET /api/v1/auth/oauth2/facebook

// Documentation
GET /swagger-ui/**
GET /v3/api-docs/**
GET /swagger-resources/**

// Health Check
GET /actuator/health
```

### Step 2: Add to SecurityConfig permitAll()

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            // Public authentication endpoints (MUST include all paths)
            .requestMatchers(HttpMethod.POST,
                "/api/v1/register",           // UserController
                "/api/v1/login",              // UserController
                "/api/v1/auth/register",      // AuthenticationController
                "/api/v1/auth/login",         // AuthenticationController
                "/api/v1/auth/refresh-token", // AuthenticationController
                "/api/v1/admin/register",     // AdminController
                "/api/v1/admin/login"         // AdminController
            ).permitAll()
            
            // OAuth2 endpoints
            .requestMatchers(HttpMethod.GET,
                "/api/v1/auth/oauth2/google",
                "/api/v1/auth/oauth2/facebook"
            ).permitAll()
            
            // Protected endpoints
            .requestMatchers("/api/v1/user/**").hasAnyRole("USER", "ADMIN")
            .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
            
            // All other requests require authentication
            .anyRequest().authenticated()
        );
    
    return http.build();
}
```

### Step 3: Verify JWT Filter Skips Public Endpoints

The `JwtAuthenticationFilter` must **not throw exceptions** for missing Authorization headers:

```java
@Override
protected void doFilterInternal(HttpServletRequest request, 
                               HttpServletResponse response,
                               FilterChain filterChain) 
        throws ServletException, IOException {
    
    final String authHeader = request.getHeader("Authorization");
    
    // If no Authorization header → SKIP AUTHENTICATION
    if (authHeader == null || authHeader.isBlank()) {
        filterChain.doFilter(request, response);  // Continue to next filter
        return;
    }
    
    // If Authorization header exists → validate JWT
    if (!authHeader.startsWith("Bearer ")) {
        filterChain.doFilter(request, response);
        return;
    }
    
    // Extract and validate JWT...
}
```

**Key Point**: The filter does NOT throw 401 - it simply skips authentication and lets `authorizeHttpRequests()` decide based on `permitAll()`.

## Complete SecurityConfig Implementation

```java
package com.example.demo_project_spring_boot.config;

import com.example.demo_project_spring_boot.security.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 6 Configuration for Stateless JWT Authentication + OAuth2 Social Login
 * 
 * Architecture:
 * 1. Stateless session management (no cookies/sessions)
 * 2. JWT tokens for authenticated requests
 * 3. Custom JWT filter for token validation
 * 4. OAuth2 support for Google/Facebook login
 * 5. Role-based authorization for protected endpoints
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtAuthenticationFilter jwtAuthFilter;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Autowired
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    @Autowired
    private OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler;

    @Autowired
    private OAuth2AuthenticationFailureHandler oauth2AuthenticationFailureHandler;

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    /**
     * Main Security Filter Chain Configuration
     * 
     * Order of execution:
     * 1. CSRF disabled for REST APIs
     * 2. CORS configured
     * 3. Session set to stateless
     * 4. Exception handlers configured
     * 5. OAuth2 login configured
     * 6. Route authorization configured
     * 7. JWT filter added before UsernamePasswordAuthenticationFilter
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // ============================================================
                // 1. DISABLE CSRF - Required for REST APIs (not for web forms)
                // ============================================================
                .csrf(AbstractHttpConfigurer::disable)

                // ============================================================
                // 2. CONFIGURE CORS
                // ============================================================
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ============================================================
                // 3. STATELESS SESSION MANAGEMENT
                // No server-side sessions, all auth state in JWT token
                // ============================================================
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ============================================================
                // 4. EXCEPTION HANDLING
                // Return JSON error responses instead of redirects
                // ============================================================
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(customAuthenticationEntryPoint)      // 401 response
                        .accessDeniedHandler(customAccessDeniedHandler))              // 403 response

                // ============================================================
                // 5. OAUTH2 LOGIN CONFIGURATION
                // Enables Google/Facebook social login
                // ============================================================
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(auth -> auth
                                .baseUri("/oauth2/authorization"))
                        .redirectionEndpoint(redirect -> redirect
                                .baseUri("/login/oauth2/code/*"))
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService))
                        .successHandler(oauth2AuthenticationSuccessHandler)
                        .failureHandler(oauth2AuthenticationFailureHandler))

                // ============================================================
                // 6. ROUTE AUTHORIZATION - CRITICAL FOR FIXING 401 ERROR
                // ============================================================
                .authorizeHttpRequests(auth -> auth

                        // Root and system endpoints
                        .requestMatchers("/", "/error", "/favicon.ico").permitAll()

                        // CORS preflight requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ===== SWAGGER & API DOCUMENTATION (Public) =====
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()

                        // ===== AUTHENTICATION ENDPOINTS (Public - No JWT Required) =====
                        // These endpoints allow users to register and login
                        // WITHOUT existing JWT token
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/register",              // UserController legacy endpoint
                                "/api/v1/login",                 // UserController legacy endpoint
                                "/api/v1/auth/register",         // AuthenticationController (modern)
                                "/api/v1/auth/login",            // AuthenticationController (modern)
                                "/api/v1/auth/refresh-token",    // Token refresh
                                "/api/v1/admin/register",        // Admin registration
                                "/api/v1/admin/login"            // Admin login
                        ).permitAll()

                        // ===== OAUTH2 ENDPOINTS (Public) =====
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/auth/oauth2/google",
                                "/api/v1/auth/oauth2/facebook"
                        ).permitAll()

                        // ===== OAUTH2 CALLBACK ENDPOINTS (Public) =====
                        .requestMatchers(
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/login"
                        ).permitAll()

                        // ===== PUBLIC PRODUCT ENDPOINTS =====
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/products/**",
                                "/api/v1/categories/**",
                                "/api/v1/reviews/**"
                        ).permitAll()

                        // ===== HEALTH CHECK =====
                        .requestMatchers("/actuator/**", "/actuator/health").permitAll()

                        // ===== ADMIN PROTECTED ENDPOINTS =====
                        .requestMatchers(HttpMethod.POST, "/api/v1/products/**", "/api/v1/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/products/**", "/api/v1/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**", "/api/v1/categories/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // ===== USER PROTECTED ENDPOINTS =====
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/user/**",
                                "/api/v1/profile/**"
                        ).hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").hasAnyRole("USER", "ADMIN")

                        // ===== DEFAULT: ALL OTHER REQUESTS REQUIRE AUTHENTICATION =====
                        .anyRequest().authenticated()
                )

                // ============================================================
                // 7. AUTHENTICATION PROVIDER & FILTERS
                // ============================================================
                // Authentication provider handles username/password validation
                .authenticationProvider(authenticationProvider())
                
                // JWT filter must be added BEFORE UsernamePasswordAuthenticationFilter
                // so JWT validation happens first
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * DAO Authentication Provider
     * Validates username/password against UserDetailsService
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    /**
     * Authentication Manager
     * Used by login endpoints to authenticate users
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * CORS Configuration
     * Allows frontend to call backend APIs from different origins
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Parse allowed origins from config
        config.setAllowedOriginPatterns(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList());

        // Allow all HTTP methods
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Allow all headers
        config.setAllowedHeaders(List.of("*"));

        // Expose Authorization header to browser
        config.setExposedHeaders(List.of("Authorization", "Content-Type"));

        // Allow credentials
        config.setAllowCredentials(true);

        // Cache CORS preflight for 1 hour
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

## Testing the Fix

### 1. Test Public /register Endpoint (Should work 200)

```bash
curl -X POST http://localhost:8080/api/v1/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123",
    "firstName": "Test",
    "lastName": "User"
  }'
```

**Expected Response 201 Created**:
```json
{
  "success": true,
  "message": "User registered successfully",
  "userId": 1,
  "username": "testuser",
  "email": "test@example.com",
  "role": "USER"
}
```

### 2. Test Public /login Endpoint (Should work 200)

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

**Expected Response 200 OK**:
```json
{
  "success": true,
  "message": "Login successful",
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "userId": 1,
  "role": "USER"
}
```

### 3. Test Protected /profile Endpoint Without Token (Should fail 401)

```bash
curl -X GET http://localhost:8080/api/v1/user/profile
```

**Expected Response 401 Unauthorized**:
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Authentication required",
  "path": "/api/v1/user/profile"
}
```

### 4. Test Protected /profile Endpoint With Token (Should work 200)

```bash
curl -X GET http://localhost:8080/api/v1/user/profile \
  -H "Authorization: Bearer <access_token_from_login>"
```

**Expected Response 200 OK**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "username": "testuser",
    "email": "test@example.com",
    "role": "USER"
  }
}
```

## Debugging Tips

### If Still Getting 401 on /register

1. **Check endpoint path**: Ensure your controller has `@RequestMapping("/api/v1")` and `@PostMapping("/register")`
2. **Verify permitAll()**: Add more specific logging in SecurityConfig
3. **Check filter order**: JWT filter should come BEFORE UsernamePasswordAuthenticationFilter
4. **Inspect request headers**: Ensure no Authorization header is being sent

```bash
# Check request headers
curl -X POST http://localhost:8080/api/v1/register -v \
  -H "Content-Type: application/json" \
  -d '{...}'
```

### Enable Spring Security Debug Logging

Add to `application.properties`:
```properties
logging.level.org.springframework.security=DEBUG
logging.level.org.springframework.security.web.FilterChainProxy=DEBUG
```

### Check Request Matches

Add debug logs in SecurityConfig:
```java
.authorizeHttpRequests(auth -> {
    auth.requestMatchers(HttpMethod.POST, "/api/v1/register").permitAll();
    // ... rest of config
    auth.anyRequest().authenticated();
})
```

## Summary of Changes

| Issue | Root Cause | Fix |
|-------|-----------|-----|
| `/api/v1/register` returns 401 | Path not in permitAll() | Added to permitAll() in SecurityConfig |
| Multiple register endpoints | Design issue | Consolidated endpoint paths |
| JWT filter rejects public endpoints | Missing Authorization header check | JWT filter skips if no header present |
| CORS blocks requests | Not configured | Added CORS configuration bean |
| OAuth2 not working | Missing endpoint configuration | Added OAuth2 endpoints to permitAll() |

## Best Practices Applied

✅ **Stateless Authentication**: No server-side sessions, using JWT tokens  
✅ **CSRF Disabled**: Appropriate for REST APIs  
✅ **CORS Configured**: Allows frontend communication  
✅ **Proper Endpoint Authorization**: Clear permitAll() vs authenticated separation  
✅ **Custom Exception Handling**: Returns JSON instead of HTML redirects  
✅ **Filter Ordering**: JWT filter before form authentication  
✅ **OAuth2 Support**: Google/Facebook social login  
✅ **Role-Based Access Control**: Different permissions for USER/ADMIN  

## Production Checklist

- [ ] Change `app.cors.allowed-origins` to specific frontend domain (not `*`)
- [ ] Change JWT secret to long random base64 string
- [ ] Enable HTTPS for all endpoints
- [ ] Set appropriate token expiration times
- [ ] Enable password hashing (BCrypt is default)
- [ ] Configure database connection pooling
- [ ] Enable request logging for audit trail
- [ ] Set up rate limiting
- [ ] Configure HTTPS redirect
- [ ] Use environment variables for secrets

