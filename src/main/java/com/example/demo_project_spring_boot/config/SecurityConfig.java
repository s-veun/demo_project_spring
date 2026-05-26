package com.example.demo_project_spring_boot.config;

import com.example.demo_project_spring_boot.security.CustomAuthenticationEntryPoint;
import com.example.demo_project_spring_boot.security.CustomAccessDeniedHandler;
import com.example.demo_project_spring_boot.security.CustomOAuth2UserService;
import com.example.demo_project_spring_boot.security.AdminRateLimitFilter;
import com.example.demo_project_spring_boot.security.OAuth2AuthenticationFailureHandler;
import com.example.demo_project_spring_boot.security.OAuth2AuthenticationSuccessHandler;
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
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Security Configuration for Spring Boot 3 + Spring Security 6
 * Supports JWT Authentication and OAuth2 Social Login
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtAuthenticationFilter jwtAuthFilter;

    @Autowired
    private AdminRateLimitFilter adminRateLimitFilter;

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

    @Autowired
    private OAuth2ClientConfig oauth2ClientConfig;

    @Value("${app.cors.allowed-origins:${cors.allowed-origins:http://localhost:3000,http://localhost:3001}}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // Disable CSRF for REST API
        http.csrf(AbstractHttpConfigurer::disable);

        // Configure CORS
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        // Stateless session management
        http.sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // Custom authentication entry point for 401 / 403 errors
        http.exceptionHandling(ex -> ex
                .authenticationEntryPoint(customAuthenticationEntryPoint)
                .accessDeniedHandler(customAccessDeniedHandler));

        // OAuth2 Login — only wired when at least one provider has real credentials
        if (oauth2ClientConfig.hasAnyRegistration()) {
            http.oauth2Login(oauth2 -> oauth2
                    .authorizationEndpoint(auth -> auth
                            .baseUri("/oauth2/authorization"))
                    .redirectionEndpoint(redirect -> redirect
                            .baseUri("/login/oauth2/code/*"))
                    .userInfoEndpoint(userInfo -> userInfo
                            .userService(customOAuth2UserService))
                    .successHandler(oauth2AuthenticationSuccessHandler)
                    .failureHandler(oauth2AuthenticationFailureHandler));
        }

        // Route Authorization Configuration
        http.authorizeHttpRequests(auth -> auth

                // Root, Error, Favicon
                .requestMatchers("/", "/error", "/favicon.ico").permitAll()

                // CORS Preflight Requests
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Swagger & OpenAPI Paths
                .requestMatchers(
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/v3/api-docs",
                        "/swagger-resources/**",
                        "/webjars/**"
                ).permitAll()

                // Logout requires an authenticated principal/session token.
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").authenticated()

                // Public Authentication Routes (password + social + refresh)
                .requestMatchers("/api/v1/auth/**").permitAll()

                // Admin login is public; admin register allows unauthenticated access for first-run bootstrap only
                // (AdminController enforces: if any admin exists, ADMIN role required)
                .requestMatchers(HttpMethod.POST, "/api/v1/admin/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/admin/register").permitAll()

                // Legacy UserController Auth Endpoints (deprecated - use /api/v1/auth/** instead)
                .requestMatchers(HttpMethod.POST,
                        "/api/v1/register",
                        "/api/v1/login"
                ).permitAll()

                // OAuth2 Authorization Endpoints
                .requestMatchers(
                        "/oauth2/**",
                        "/login/**"
                ).permitAll()

                // Public GET Routes (Products/Categories)
                .requestMatchers(HttpMethod.GET,
                        "/api/v1/products/**",
                        "/api/v1/categories/**",
                        "/api/v1/reviews/**",
                        "/api/v1/search/**"

                ).permitAll()

                // Static file uploads
                .requestMatchers("/uploads/**").permitAll()

                // Public health/info endpoints only
                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")

                // ADMIN Role Routes
                .requestMatchers(HttpMethod.POST, "/api/v1/products/**", "/api/v1/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/products/**", "/api/v1/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**", "/api/v1/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/popularity/update-all-scores").hasRole("ADMIN")
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                // Cart Routes – accessible by both USER and ADMIN (ownership enforced in CartController)
                .requestMatchers("/api/v1/cart/**").authenticated()

                // Order Routes – authenticated; ownership enforced at controller/service level
                .requestMatchers("/api/v1/orders/**").authenticated()

                // Wishlist Routes – authenticated; ownership enforced in WishlistController
                .requestMatchers("/api/v1/wishlist/**").authenticated()

                // USER-only routes
                .requestMatchers("/api/v1/user/**").hasRole("USER")
                .requestMatchers("/api/v1/users/**").authenticated()
                .requestMatchers("/api/v1/popularity/user/**").hasRole("USER")
                .requestMatchers(HttpMethod.GET, "/api/v1/profile/**", "/api/v1/me").hasRole("USER")

                // Public popularity analytics endpoints (excluding user-scoped and admin update route)
                .requestMatchers(HttpMethod.GET, "/api/v1/popularity/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/popularity/view/**").permitAll()

                // Any other request requires authentication
                .anyRequest().authenticated()
        );

        // Authentication Provider and JWT Filter
        http.authenticationProvider(authenticationProvider())
                .addFilterBefore(adminRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Configure CORS for frontend integration
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        List<String> configuredOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();

        List<String> wildcardOrigins = configuredOrigins.stream()
                .filter(value -> value.contains("*"))
                .toList();

        List<String> exactOrigins = configuredOrigins.stream()
                .filter(value -> !value.contains("*"))
                .toList();

        // Keep local admin/user frontends working without requiring wildcard CORS.
        LinkedHashSet<String> exactOriginSet = new LinkedHashSet<>(exactOrigins);
        exactOriginSet.add("http://localhost:3000");
        exactOriginSet.add("http://localhost:3001");
        exactOriginSet.add("http://127.0.0.1:3000");
        exactOriginSet.add("http://127.0.0.1:3001");

        config.setAllowedOrigins(List.copyOf(exactOriginSet));
        config.setAllowedOriginPatterns(wildcardOrigins);

        // Allow all methods
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Allow commonly used headers, including credential-related browser headers.
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With",
                "Cookie",
                "Set-Cookie"
        ));

        // Expose auth headers for clients that inspect tokens/cookies.
        config.setExposedHeaders(List.of("Authorization", "Content-Type", "Set-Cookie"));

        // Required for refresh-token cookie flows used by browser frontends.
        config.setAllowCredentials(true);

        // Cache CORS for 1 hour
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

}