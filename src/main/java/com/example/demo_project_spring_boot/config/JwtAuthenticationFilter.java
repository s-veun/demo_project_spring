package com.example.demo_project_spring_boot.config;

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

import java.io.IOException;

@Component
@RequiredArgsConstructor // វានឹងបង្កើត Constructor ឱ្យតែអថេរណាដែលមានពាក្យ 'final' ប៉ុណ្ណោះ
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // 🌟 ត្រូវតែមានពាក្យ final ដើម្បីឱ្យ Constructor Injection ដំណើរការ
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // ១. ត្រួតពិនិត្យ Header
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // ២. ទាញយក Token និង Username
        jwt = authHeader.substring(7);
        username = jwtService.extractUsername(jwt);

        // ៣. បញ្ជាក់អត្តសញ្ញាណ (Authentication)
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            if (jwtService.isTokenValid(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities() // Role ស្ថិតនៅត្រង់នេះ
                );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // បញ្ជូនទៅក្នុង Security Context
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // ៤. បញ្ជូនទៅ Filter បន្ទាប់
        filterChain.doFilter(request, response);
    }
}