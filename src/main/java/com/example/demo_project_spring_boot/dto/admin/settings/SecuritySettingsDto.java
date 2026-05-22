package com.example.demo_project_spring_boot.dto.admin.settings;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SecuritySettingsDto {

    @NotNull
    @Min(value = 60, message = "JWT expiration should be at least 60 seconds")
    @Max(value = 604800, message = "JWT expiration should be at most 7 days")
    private Integer jwtExpirationSeconds;

    @NotNull
    @Min(value = 6)
    @Max(value = 64)
    private Integer passwordMinLength;

    @NotNull
    private Boolean requireUppercase;

    @NotNull
    private Boolean requireLowercase;

    @NotNull
    private Boolean requireNumber;

    @NotNull
    private Boolean requireSpecialCharacter;

    @NotNull
    @Min(3)
    @Max(20)
    private Integer loginAttemptLimit;

    @NotNull
    @Min(5)
    @Max(1440)
    private Integer sessionTimeoutMinutes;

    @NotNull
    private Boolean twoFactorEnabled;

    @NotNull
    private Boolean corsEnabled;

    private List<String> allowedOrigins;

    @NotNull
    @Min(10)
    @Max(10000)
    private Integer apiRateLimitPerMinute;
}

