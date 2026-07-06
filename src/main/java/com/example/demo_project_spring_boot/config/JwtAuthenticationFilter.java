package com.example.demo_project_spring_boot.config;

import com.example.demo_project_spring_boot.model.User;
import com.example.demo_project_spring_boot.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_FAILURE_REASON_ATTR = "auth.failure.reason";

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final List<String> SKIP_PATHS = List.of(
            "/api/v1/auth/**",
            "/api/v1/admin/login",
            "/api/v1/admin/register",
            "/oauth2/**",
            "/login/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**"
    );

    private static final List<String> PUBLIC_GET_PATHS = List.of(
            "/api/v1/products/**",
            "/api/v1/categories/**",
            "/api/v1/reviews/**",
            "/api/v1/search/**",
            "/api/v1/popularity/**",
            "/uploads/**",
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info"
    );

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String requestPath = normalizePath(request);

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        if ("GET".equalsIgnoreCase(request.getMethod())
                && PUBLIC_GET_PATHS.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, requestPath))) {
            return true;
        }

        return SKIP_PATHS.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, requestPath));
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String requestPath = normalizePath(request);
        final String jwt = extractAccessToken(request);

        if (!StringUtils.hasText(jwt)) {
            log.debug("[JWT] No access token found for request: {} {}", request.getMethod(), requestPath);
            request.setAttribute(AUTH_FAILURE_REASON_ATTR, "Access token is missing");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final Long userId = jwtService.extractUserId(jwt);
            String username = jwtService.extractUsername(jwt);

            log.debug("[JWT] Processing token for request: {} {} | extracted user ID: {}, username: {}",
                    request.getMethod(), requestPath, userId, username);

            User user = null;
            if (!StringUtils.hasText(username) && userId != null) {
                user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    username = user.getUsername();
                    log.debug("[JWT] Recovered username '{}' from userId={} for request: {}",
                            username, userId, requestPath);
                }
            }

            if (!StringUtils.hasText(username)) {
                log.warn("[JWT] Token has no resolvable subject for request: {}", requestPath);
                request.setAttribute(AUTH_FAILURE_REASON_ATTR, "JWT token has no subject");
                filterChain.doFilter(request, response);
                return;
            }

            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                log.debug("[JWT] SecurityContext already authenticated for user: {}", userId);
                filterChain.doFilter(request, response);
                return;
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (userDetails == null) {
                log.warn("[JWT] UserDetails not found for username: {} | path: {}", username, requestPath);
                request.setAttribute(AUTH_FAILURE_REASON_ATTR, "User not found");
                filterChain.doFilter(request, response);
                return;
            }

            // Check JWT structural validity and expiry
            boolean tokenValid = StringUtils.hasText(jwtService.extractUsername(jwt))
                    ? jwtService.isTokenValid(jwt, userDetails)
                    : !jwtService.isTokenExpired(jwt);
            boolean isAccessType = jwtService.isAccessToken(jwt);
            log.debug("[JWT] Token validation - username match+not expired: {}, isAccessType: {} | user: {}",
                    tokenValid, isAccessType, username);

            if (!tokenValid) {
                log.warn("[JWT] Token failed validity check (expired or username mismatch) for user: {} | path: {}",
                        username, requestPath);
                request.setAttribute(AUTH_FAILURE_REASON_ATTR, "JWT token is expired or username mismatch");
                filterChain.doFilter(request, response);
                return;
            }

            if (!isAccessType) {
                log.warn("[JWT] Token is not an access token (may be a refresh token) for user: {} | path: {}",
                        username, requestPath);
                request.setAttribute(AUTH_FAILURE_REASON_ATTR, "Provided token is not an access token");
                filterChain.doFilter(request, response);
                return;
            }

            // Check single-session token binding (DB stored token must match)
            if (user == null) {
                user = userRepository.findByUsername(username).orElse(null);
            }

            String activeAccessToken = user != null ? user.getAccessToken() : null;

            boolean tokenMatchesDb = (activeAccessToken == null
                    || activeAccessToken.isBlank()
                    || Objects.equals(jwt, activeAccessToken));

            log.debug("[JWT] DB token binding check - dbTokenPresent: {}, matches: {} | user: {}",
                    (activeAccessToken != null && !activeAccessToken.isBlank()), tokenMatchesDb, username);

            if (!tokenMatchesDb) {
                log.warn("[JWT] Token does not match stored active session token for user: {} | path: {} "
                        + "| This usually means the user logged in again or refreshed their token on another device.",
                        username, requestPath);
                request.setAttribute(AUTH_FAILURE_REASON_ATTR,
                        "JWT token is revoked. Please login again to get a fresh token.");
                filterChain.doFilter(request, response);
                return;
            }

            // All checks passed – set authentication in SecurityContext
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

            log.debug("[JWT] Authentication SUCCESS for user: {} | authorities: {} | path: {}",
                    username, userDetails.getAuthorities(), requestPath);

        } catch (ExpiredJwtException ex) {
            log.warn("[JWT] Token expired for request: {} | expiredAt: {}",
                    requestPath, ex.getClaims() != null ? ex.getClaims().getExpiration() : "unknown");
            request.setAttribute(AUTH_FAILURE_REASON_ATTR, "JWT token is expired");
            SecurityContextHolder.clearContext();
        } catch (JwtException ex) {
            log.warn("[JWT] Malformed or invalid signature token for request: {} | error: {}",
                    requestPath, ex.getMessage());
            request.setAttribute(AUTH_FAILURE_REASON_ATTR, "JWT token is malformed or signature is invalid");
            SecurityContextHolder.clearContext();
        } catch (Exception ex) {
            log.error("[JWT] Unexpected error during token validation for request: {} | error: {}",
                    requestPath, ex.getMessage(), ex);
            request.setAttribute(AUTH_FAILURE_REASON_ATTR, "JWT token could not be validated");
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String normalizePath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();

        if (StringUtils.hasText(contextPath) && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }

        return path;
    }

    private String extractAccessToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader)) {
            if (!authHeader.startsWith("Bearer ")) {
                log.debug("[JWT] Authorization header present but does not start with 'Bearer ': {}",
                        authHeader.substring(0, Math.min(authHeader.length(), 20)));
                return null;
            }
            String bearerToken = authHeader.substring(7).trim();
            if (!bearerToken.isBlank()) {
                return bearerToken;
            }
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (cookie == null || cookie.getName() == null) {
                continue;
            }
            if ("accessToken".equalsIgnoreCase(cookie.getName())
                    || "te_access_token".equalsIgnoreCase(cookie.getName())
                    || "admin_access_token".equalsIgnoreCase(cookie.getName())) {
                String value = cookie.getValue();
                if (StringUtils.hasText(value)) {
                    log.debug("[JWT] Token extracted from cookie: {}", cookie.getName());
                    return value.trim();
                }
            }
        }

        return null;
    }
}
