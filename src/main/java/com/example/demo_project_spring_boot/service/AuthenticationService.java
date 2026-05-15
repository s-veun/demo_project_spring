package com.example.demo_project_spring_boot.service;

import com.example.demo_project_spring_boot.dto.*;
import com.example.demo_project_spring_boot.model.User;

public interface AuthenticationService {

    /**
     * Register a new user with email and password
     */
    RegisterResponse registerUser(RegisterRequest request);

    /**
     * Authenticate user with email/username and password
     */
    LoginResponse loginUser(LoginRequest request);

    /**
     * Refresh access token using refresh token
     */
    RefreshTokenResponse refreshToken(RefreshTokenRequest request);

    /**
     * Logout user (optional - can be handled via frontend token deletion)
     */
    void logoutUser(String username);

    void revokeSession(String accessToken, String refreshToken);

    /**
     * Validate JWT token
     */
    boolean validateToken(String token);

    /**
     * Get user information from JWT token
     */
    User getUserFromToken(String token);
}

