package com.example.demo_project_spring_boot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateAdminProfileRequest {
    private String email;
    private String phoneNumber;
    private String firstName;
    private String lastName;
    private String profileImageUrl;
}
