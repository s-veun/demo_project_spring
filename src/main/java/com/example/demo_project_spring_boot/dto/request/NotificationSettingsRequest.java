package com.example.demo_project_spring_boot.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NotificationSettingsRequest {
    @NotNull
    private Boolean emailNotificationsEnabled;

    @NotNull
    private Boolean smsNotificationsEnabled;

    @NotNull
    private Boolean marketingNotificationsEnabled;

    @NotNull
    private Boolean securityAlertsEnabled;
}

