package com.example.demo_project_spring_boot.dto.request;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateUserProfileRequest {

    @Size(max = 120, message = "Full name must be at most 120 characters")
    private String fullName;

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "Username has invalid characters")
    private String username;

    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Phone number format is invalid")
    private String phoneNumber;

    @Pattern(regexp = "^(MALE|FEMALE|OTHER)?$", message = "Gender must be MALE, FEMALE, or OTHER")
    private String gender;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @Size(max = 600, message = "Bio must be at most 600 characters")
    private String bio;

    @Size(max = 100, message = "Country must be at most 100 characters")
    private String country;

    @Size(max = 100, message = "City must be at most 100 characters")
    private String city;

    @Size(max = 255, message = "Facebook URL too long")
    private String facebookUrl;

    @Size(max = 120, message = "Telegram handle too long")
    private String telegramHandle;

    @Size(max = 255, message = "Instagram URL too long")
    private String instagramUrl;

    @Size(max = 255, message = "LinkedIn URL too long")
    private String linkedInUrl;
}

