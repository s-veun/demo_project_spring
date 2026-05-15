package com.example.demo_project_spring_boot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Refresh Token Request DTO
 * Used to generate new access token using refresh token
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RefreshTokenRequest {
    private String refreshToken;
}

