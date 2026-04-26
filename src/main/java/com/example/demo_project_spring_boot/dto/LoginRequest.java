package com.example.demo_project_spring_boot.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}
