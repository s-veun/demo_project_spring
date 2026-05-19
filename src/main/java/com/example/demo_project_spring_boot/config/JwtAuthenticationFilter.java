package com.example.demo_project_spring_boot.config;

import com.example.demo_project_spring_boot.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();

        if (StringUtils.hasText(contextPath) && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }

        final String requestPath = path;
        return SKIP_PATHS.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, requestPath));
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String jwt = extractAccessToken(request);

        if (!StringUtils.hasText(jwt)) {
            request.setAttribute(AUTH_FAILURE_REASON_ATTR, "Access token is missing");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String username = jwtService.extractUsername(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                String activeAccessToken = userRepository.findByUsername(username)
                        .map(com.example.demo_project_spring_boot.model.User::getAccessToken)
                        .orElse(null);

                if (jwtService.isTokenValid(jwt, userDetails)
                        && jwtService.isAccessToken(jwt)
                        && (activeAccessToken == null || activeAccessToken.isBlank() || Objects.equals(jwt, activeAccessToken))) {

                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authenticationToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext()
                            .setAuthentication(authenticationToken);
                } else {
                    request.setAttribute(AUTH_FAILURE_REASON_ATTR, "JWT token is invalid, expired, or revoked");
                }
            }

        } catch (ExpiredJwtException ex) {
            request.setAttribute(AUTH_FAILURE_REASON_ATTR, "JWT token is expired");
            SecurityContextHolder.clearContext();
        } catch (JwtException ex) {
            request.setAttribute(AUTH_FAILURE_REASON_ATTR, "JWT token is malformed or signature is invalid");
            SecurityContextHolder.clearContext();
        } catch (Exception ex) {
            request.setAttribute(AUTH_FAILURE_REASON_ATTR, "JWT token could not be validated");
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String extractAccessToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader)) {
            if (!authHeader.startsWith("Bearer ")) {
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
                    return value.trim();
                }
            }
        }

        return null;
    }
}