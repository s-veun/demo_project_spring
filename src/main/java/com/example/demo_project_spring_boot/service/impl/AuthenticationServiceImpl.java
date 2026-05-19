package com.example.demo_project_spring_boot.service.impl;

import com.example.demo_project_spring_boot.config.JwtService;
import com.example.demo_project_spring_boot.dto.*;
import com.example.demo_project_spring_boot.Enum.AuthProvider;
import com.example.demo_project_spring_boot.exception.BadRequestException;
import com.example.demo_project_spring_boot.exception.DuplicateResourceException;
import com.example.demo_project_spring_boot.exception.UnauthorizedException;
import com.example.demo_project_spring_boot.model.User;
import com.example.demo_project_spring_boot.repository.UserRepository;
import com.example.demo_project_spring_boot.service.AuthenticationService;
import com.example.demo_project_spring_boot.Enum.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Authentication Service Implementation
 * Handles user registration, login, and JWT token management
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Override
    public RegisterResponse registerUser(RegisterRequest request) {
        log.info("Registering new user: {}", request.getUsername());

        // Check if user already exists
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            log.warn("User already exists: {}", request.getUsername());
            throw new DuplicateResourceException("Username already registered");
        }

        // Check if email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("Email already registered: {}", request.getEmail());
            throw new DuplicateResourceException("Email already registered");
        }

        // Create new user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .provider(AuthProvider.LOCAL)
                .isOAuth2Linked(false)
                .role(Role.USER)
                .isEnabled(true)
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully: {}", user.getId());

        return RegisterResponse.builder()
                .success(true)
                .message("User registered successfully")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    @Override
    public LoginResponse loginUser(LoginRequest request) {
        log.info("Attempting login for user: {}", request.getUsername());

        try {
            String loginId = request.getUsername() == null ? "" : request.getUsername().trim();

            // Authenticate user
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginId, request.getPassword())
            );

            // Find user in database
            Optional<User> userOpt = userRepository.findByUsername(loginId)
                    .or(() -> userRepository.findByEmail(loginId));
            if (userOpt.isEmpty()) {
                throw new UnauthorizedException("Invalid username or password");
            }

            User user = userOpt.get();

            // Check if user is enabled
            if (!user.getIsEnabled()) {
                log.warn("Disabled user attempted to login: {}", user.getId());
                throw new UnauthorizedException("Account is disabled");
            }

            // Update last login time
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            // Generate JWT tokens
            String accessToken = jwtService.generateAccessToken(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getRole().name()
            );

            String refreshToken = jwtService.generateRefreshToken(user.getUsername(), user.getId());
            user.setAccessToken(accessToken);
            user.setRefreshToken(refreshToken);
            userRepository.save(user);

            log.info("User login successful: {}", user.getId());

            return LoginResponse.builder()
                    .success(true)
                    .message("Login successful")
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .userId(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .profileImageUrl(user.getProfileImageUrl())
                    .role(user.getRole().name())
                    .expiresIn(jwtService.getAccessTokenExpirationSeconds())
                    .build();

        } catch (BadCredentialsException e) {
            log.warn("Invalid credentials for user: {}", request.getUsername());
            throw new UnauthorizedException("Invalid username or password");
        } catch (UnauthorizedException e) {
            log.warn("Login rejected for user {}: {}", request.getUsername(), e.getMessage());
            throw e;
        } catch (IllegalStateException e) {
            log.error("Error during login", e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during login", e);
            throw new BadRequestException("Login failed");
        }
    }

    @Override
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        log.info("Attempting to refresh token");

        String token = request.getRefreshToken();
        if (!StringUtils.hasText(token)) {
            throw new BadRequestException("Refresh token is required");
        }

        if (!jwtService.isRefreshToken(token)) {
            throw new BadRequestException("Invalid token type");
        }

        if (jwtService.isTokenExpired(token)) {
            log.warn("Refresh token is expired");
            throw new UnauthorizedException("Refresh token is expired");
        }

        String username = jwtService.extractUsername(token);
        Long userId = jwtService.extractUserId(token);

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new UnauthorizedException("User not found");
        }

        User user = userOpt.get();
        if (!token.equals(user.getRefreshToken())) {
            throw new UnauthorizedException("Refresh token is revoked or does not match active session");
        }

        String newAccessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name()
        );
        user.setAccessToken(newAccessToken);
        userRepository.save(user);

        log.info("Token refreshed successfully for user: {}", userId);

        return RefreshTokenResponse.builder()
                .success(true)
                .message("Token refreshed successfully")
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationSeconds())
                .build();
    }

    @Override
    public void logoutUser(String username) {
        log.info("User logged out: {}", username);
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setAccessToken(null);
            user.setRefreshToken(null);
            userRepository.save(user);
        });
    }

    @Override
    public void revokeSession(String accessToken, String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            userRepository.findByRefreshToken(refreshToken).ifPresent(user -> {
                user.setRefreshToken(null);
                user.setAccessToken(null);
                userRepository.save(user);
            });
            return;
        }
        if (accessToken != null && !accessToken.isBlank()) {
            userRepository.findByAccessToken(accessToken).ifPresent(user -> {
                user.setAccessToken(null);
                user.setRefreshToken(null);
                userRepository.save(user);
            });
        }
    }

    @Override
    public boolean validateToken(String token) {
        try {
            jwtService.extractUsername(token);
            return !jwtService.isTokenExpired(token);
        } catch (Exception e) {
            log.debug("Token validation failed", e);
            return false;
        }
    }

    @Override
    public User getUserFromToken(String token) {
        try {
            String username = jwtService.extractUsername(token);
            Optional<User> userOpt = userRepository.findByUsername(username);
            return userOpt.orElse(null);
        } catch (Exception e) {
            log.debug("Error extracting user from token", e);
            return null;
        }
    }
}

