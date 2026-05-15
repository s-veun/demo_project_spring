package com.example.demo_project_spring_boot.config;

import com.example.demo_project_spring_boot.security.CustomAuthenticationEntryPoint;
import com.example.demo_project_spring_boot.security.OAuth2AuthenticationFailureHandler;
import com.example.demo_project_spring_boot.security.OAuth2AuthenticationSuccessHandler;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Security Configuration for Spring Boot 3 + Spring Security 6
 * Supports JWT Authentication and OAuth2 Social Login
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
    private OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler;

    @Autowired
    private OAuth2AuthenticationFailureHandler oauth2AuthenticationFailureHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // Disable CSRF for REST API
                .csrf(AbstractHttpConfigurer::disable)

                // Configure CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Stateless session management
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Custom authentication entry point for 401 errors
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(customAuthenticationEntryPoint))

                // OAuth2 Login Configuration
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(auth -> auth
                                .baseUri("/oauth2/authorize"))
                        .redirectionEndpoint(redirect -> redirect
                                .baseUri("/login/oauth2/code/*"))
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(oidcUserService()))
                        .successHandler(oauth2AuthenticationSuccessHandler)
                        .failureHandler(oauth2AuthenticationFailureHandler))

                // Route Authorization Configuration
                .authorizeHttpRequests(auth -> auth

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

                        // Public Authentication Routes
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh-token",
                                "/api/v1/admin/register",
                                "/api/v1/admin/login"
                        ).permitAll()

                        // OAuth2 Authorization Endpoints
                        .requestMatchers(
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/login"
                        ).permitAll()

                        // Public GET Routes (Products/Categories)
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/products/**",
                                "/api/v1/categories/**",
                                "/api/v1/reviews/**"
                        ).permitAll()

                        // Actuator Health Check
                        .requestMatchers("/actuator/**", "/actuator/health").permitAll()

                        // ADMIN Role Routes
                        .requestMatchers(HttpMethod.POST, "/api/v1/products/**", "/api/v1/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/products/**", "/api/v1/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**", "/api/v1/categories/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // Authenticated User Routes
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/user/**",
                                "/api/v1/profile/**",
                                "/api/v1/auth/logout"
                        ).hasAnyRole("USER", "ADMIN")

                        // Any other request requires authentication
                        .anyRequest().authenticated()
                )

                // Authentication Provider and Filters
                .authenticationProvider(authenticationProvider())
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

        // Allow origins (should be configured via environment variables in production)
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:3001",
                "http://localhost:8080",
                "https://demoprojectspring-production.up.railway.app",
                "https://*.vercel.app"
        ));

        // Allow all methods
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Allow all headers
        config.setAllowedHeaders(List.of("*"));

        // Expose Authorization header to frontend
        config.setExposedHeaders(List.of("Authorization", "Content-Type"));

        // Allow credentials (cookies, authorization headers, etc.)
        config.setAllowCredentials(true);

        // Cache CORS for 1 hour
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * OIDC User Service for OAuth2 Login
     */
    @Bean
    public OidcUserService oidcUserService() {
        return new OidcUserService();
    }
}