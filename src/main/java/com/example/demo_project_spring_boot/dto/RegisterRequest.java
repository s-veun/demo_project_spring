package com.example.demo_project_spring_boot.dto;


import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
}
