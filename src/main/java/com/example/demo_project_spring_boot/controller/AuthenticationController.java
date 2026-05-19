package com.example.demo_project_spring_boot.controller;

import com.example.demo_project_spring_boot.dto.*;
import com.example.demo_project_spring_boot.exception.BadRequestException;
import com.example.demo_project_spring_boot.exception.DuplicateResourceException;
import com.example.demo_project_spring_boot.exception.UnauthorizedException;
import com.example.demo_project_spring_boot.config.JwtService;
import com.example.demo_project_spring_boot.security.TokenCookieService;
import com.example.demo_project_spring_boot.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

/**
 * Authentication Controller
 * Handles user registration, login, token refresh, and logout
 * Endpoints: POST /api/v1/auth/...
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Social Authentication APIs", description = "Continue with Google, refresh token, secure logout, and profile flow integration")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final TokenCookieService tokenCookieService;
    private final JwtService jwtService;

    /**
     * Register new user with email and password
     * POST /api/v1/auth/register
     */
    @PostMapping("/register")
    @Hidden
    @Operation(summary = "Register new user", description = "Create a new user account with email and password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = RegisterResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or user already exists"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest request) {
        try {
            log.info("Register endpoint called for user: {}", request.getUsername());

            // Validate input
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Username is required"));
            }

            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Email is required"));
            }

            if (request.getPassword() == null || request.getPassword().length() < 6) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Password must be at least 6 characters"));
            }

            RegisterResponse response = authenticationService.registerUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("Registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (DuplicateResourceException e) {
            log.warn("Registration conflict: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (BadRequestException e) {
            log.warn("Registration request invalid: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error during registration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Registration failed"));
        }
    }

    /**
     * Login with email/username and password
     * POST /api/v1/auth/login
     */
    @PostMapping("/login")
    @Hidden
    @Operation(summary = "Login user", description = "Authenticate user with email/username and password, returns JWT tokens")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad credentials or validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid credentials"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest request, HttpServletResponse httpResponse) {
        try {
            log.info("Login endpoint called for user: {}", request.getUsername());

            // Validate input
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Username is required"));
            }

            if (request.getPassword() == null || request.getPassword().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Password is required"));
            }

            LoginResponse response = authenticationService.loginUser(request);
            tokenCookieService.writeRefreshTokenCookie(
                    httpResponse,
                    response.getRefreshToken(),
                    jwtService.getRefreshTokenExpirationSeconds()
            );
            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            log.warn("Login failed due to invalid credentials for user: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Invalid credentials"));
        } catch (UnauthorizedException e) {
            log.warn("Login unauthorized for user {}: {}", request.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (BadRequestException e) {
            log.warn("Login bad request for user {}: {}", request.getUsername(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("Login request rejected for user {}: {}", request.getUsername(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during login", e);
            Throwable root = e;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            if (root instanceof IllegalStateException) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of(
                                "success", false,
                                "message", "JWT configuration error. Verify JWT_SECRET and JWT_SECRET_FORMAT"
                        ));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Login failed"));
        }
    }

    /**
     * Refresh access token using refresh token
     * POST /api/v1/auth/refresh-token
     */
    @PostMapping({"/refresh-token", "/refresh"})
    @Operation(summary = "Refresh access token", description = "Generate new access token using refresh token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully",
                    content = @Content(schema = @Schema(implementation = RefreshTokenResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid or expired refresh token"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> refreshToken(
            @RequestBody(required = false) RefreshTokenRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        try {
            log.info("Refresh token endpoint called");

            String resolvedRefreshToken = extractRefreshToken(request, authorizationHeader, httpRequest);
            if (!StringUtils.hasText(resolvedRefreshToken)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Refresh token is required"));
            }

            RefreshTokenResponse response = authenticationService.refreshToken(
                    RefreshTokenRequest.builder().refreshToken(resolvedRefreshToken).build()
            );
            tokenCookieService.writeRefreshTokenCookie(
                    httpResponse,
                    response.getRefreshToken(),
                    jwtService.getRefreshTokenExpirationSeconds()
            );
            return ResponseEntity.ok(ApiResult.<RefreshTokenResponse>builder()
                    .success(true)
                    .message("Refresh access token successful")
                    .data(response)
                    .build());

        } catch (BadRequestException e) {
            log.warn("Token refresh bad request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResult.builder().success(false).message(e.getMessage()).build());
        } catch (Exception e) {
            if (e instanceof UnauthorizedException) {
                log.warn("Token refresh unauthorized: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResult.builder().success(false).message(e.getMessage()).build());
            }
            log.warn("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResult.builder().success(false).message(e.getMessage()).build());
        }
    }

    /**
     * Logout user (optional endpoint)
     * POST /api/v1/auth/logout
     * In stateless JWT authentication, logout is typically handled by frontend by deleting token
     */
    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Logout user session (optional - JWT is stateless)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logout successful"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> logoutUser(
            @RequestBody(required = false) LogoutRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication,
            HttpServletResponse httpResponse
    ) {
        try {
            String accessToken = extractBearerToken(authorizationHeader);
            String refreshToken = request != null ? request.getRefreshToken() : null;
            authenticationService.revokeSession(accessToken, refreshToken);

            if (authentication != null && authentication.isAuthenticated()) {
                authenticationService.logoutUser(authentication.getName());
            }

            tokenCookieService.clearRefreshTokenCookie(httpResponse);

            log.info("User logout completed");

            return ResponseEntity.ok(ApiResult.builder().success(true).message("Logout successful").build());

        } catch (Exception e) {
            log.error("Error during logout", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResult.builder().success(false).message("Logout failed").build());
        }
    }


    /**
     * Google OAuth2 Login endpoint (informational)
     * The actual OAuth2 flow is handled by Spring Security.
     * Frontend should redirect to /oauth2/authorization/google.
     */
    @GetMapping("/oauth2/google")
    @Operation(summary = "Continue with Google", description = "Returns authorization URL for Google OAuth2 login")
    public ResponseEntity<?> getGoogleLoginInfo() {
        return ResponseEntity.ok(ApiResult.builder()
                .success(true)
                .message("Continue with Google")
                .data(Map.of("authorizationUri", "/oauth2/authorization/google"))
                .build());
    }

    @GetMapping("/oauth2/facebook")
    @Operation(summary = "Continue with Facebook", description = "Returns authorization URL for Facebook OAuth2 login")
    public ResponseEntity<?> getFacebookLoginInfo() {
        return ResponseEntity.ok(ApiResult.builder()
                .success(true)
                .message("Continue with Facebook")
                .data(Map.of("authorizationUri", "/oauth2/authorization/facebook"))
                .build());
    }

    private String extractBearerToken(String authHeader) {
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7).trim();
    }

    private String extractRefreshToken(
            RefreshTokenRequest request,
            String authorizationHeader,
            HttpServletRequest httpRequest
    ) {
        if (request != null && StringUtils.hasText(request.getRefreshToken())) {
            return request.getRefreshToken().trim();
        }

        String bearerToken = extractBearerToken(authorizationHeader);
        if (StringUtils.hasText(bearerToken)) {
            return bearerToken;
        }

        Cookie[] cookies = httpRequest.getCookies();
        if (cookies == null || cookies.length == 0) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (cookie == null || !StringUtils.hasText(cookie.getName())) {
                continue;
            }
            if ("refreshToken".equalsIgnoreCase(cookie.getName())
                    || "te_refresh_token".equalsIgnoreCase(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}

