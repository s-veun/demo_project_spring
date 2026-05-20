package com.example.demo_project_spring_boot.dto;

import com.example.demo_project_spring_boot.Enum.Role;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String phoneNumber;
    private String bio;
    private String gender;
    private LocalDate dateOfBirth;
    private String country;
    private String city;
    private String profileImageUrl;
    private Boolean emailVerified;
    private Boolean phoneVerified;
    private LocalDateTime emailVerifiedAt;
    private LocalDateTime phoneVerifiedAt;
    private String facebookUrl;
    private String telegramHandle;
    private String instagramUrl;
    private String linkedInUrl;
    private Boolean emailNotificationsEnabled;
    private Boolean smsNotificationsEnabled;
    private Boolean marketingNotificationsEnabled;
    private Boolean securityAlertsEnabled;
    private Role role;
    private Boolean isEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private Long totalOrders;
    private Long wishlistCount;
    private Long reviewCount;
    private Integer profileCompletion;
    private List<AddressDto> addresses;
}
