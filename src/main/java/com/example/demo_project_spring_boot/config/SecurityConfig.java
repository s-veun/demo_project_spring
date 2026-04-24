package com.example.demo_project_spring_boot.config;

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
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtAuthenticationFilter jwtAuthFilter;

    @Autowired
    private UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // បិទ CSRF
                .csrf(AbstractHttpConfigurer::disable)

                // កំណត់ Session ជា STATELESS (ព្រោះយើងប្រើ JWT)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 🌟 ចាប់ផ្តើមកំណត់សិទ្ធិទាំងអស់នៅក្នុង Block តែមួយនេះ
                .authorizeHttpRequests(auth -> auth

                        // ១. ផ្លូវដែលអ្នកណាក៏អាចចូលបាន (Public Endpoints)
                        .requestMatchers(HttpMethod.POST, "/api/v1/register", "/api/v1/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()

                        // ២. ផ្លូវដែលតម្រូវឱ្យមានសិទ្ធិជា ADMIN (សម្រាប់ Products)
                        .requestMatchers(HttpMethod.POST, "/api/v1/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**").hasRole("ADMIN")

                        // ៣. ផ្លូវដែលតម្រូវឱ្យមានសិទ្ធិជា ADMIN (សម្រាប់ Categories)
                        .requestMatchers(HttpMethod.POST, "/api/v1/categories").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/categories/**").hasRole("ADMIN")

                        // ៤. ផ្លូវផ្សេងៗសម្រាប់ ADMIN
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // ៥. ផ្លូវដែលអាចចូលបានទាំង USER និង ADMIN
                        .requestMatchers("/api/v1/me").hasAnyRole("USER", "ADMIN")

                        // 🌟 ៦. ច្បាប់ចុងក្រោយគេបង្អស់៖ រាល់ផ្លូវផ្សេងពីនេះ ត្រូវតែមាន Token (Login រួច)
                        .anyRequest().authenticated()
                )

                // ភ្ជាប់ Provider និង Filter របស់ JWT
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        // បង្កើត Object DaoAuthenticationProvider ដោយទទេ រួចទើប Set តម្លៃឱ្យវា
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);

        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}