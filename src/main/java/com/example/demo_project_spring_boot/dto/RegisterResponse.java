package com.example.demo_project_spring_boot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Register Response DTO
 * Returned after successful user registration
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegisterResponse {
    private boolean success;
    private String message;
    private Long userId;
    private String username;
    private String email;
    private String role;
}

