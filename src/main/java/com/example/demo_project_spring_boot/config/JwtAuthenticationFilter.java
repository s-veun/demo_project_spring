package com.example.demo_project_spring_boot.config;

import com.example.demo_project_spring_boot.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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

        final String authHeader = request.getHeader("Authorization");

        // 1. No Authorization header
        if (authHeader == null || authHeader.isBlank()) {
            request.setAttribute(AUTH_FAILURE_REASON_ATTR, "Authorization header is missing");
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Authorization header exists but not Bearer token
        if (!authHeader.startsWith("Bearer ")) {
            request.setAttribute(AUTH_FAILURE_REASON_ATTR, "Authorization header must use Bearer token");
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extract token
        final String jwt = authHeader.substring(7).trim();

        // 4. Empty token
        if (jwt.isBlank()) {
            request.setAttribute(AUTH_FAILURE_REASON_ATTR, "Bearer token is empty");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 5. Extract username safely
            final String username = jwtService.extractUsername(jwt);

            // 6. Authenticate only if not already authenticated
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                String activeAccessToken = userRepository.findByUsername(username)
                        .map(com.example.demo_project_spring_boot.model.User::getAccessToken)
                        .orElse(null);

                // 7. Validate token
                if (jwtService.isTokenValid(jwt, userDetails)
                        && jwtService.isAccessToken(jwt)
                        && Objects.equals(jwt, activeAccessToken)) {

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authToken);
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

        // 8. Continue filter chain
        filterChain.doFilter(request, response);
    }
}