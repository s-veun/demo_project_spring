package com.example.demo_project_spring_boot.dto.admin.settings;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NotificationSettingsDto {

    @NotNull
    private Boolean emailNotificationsEnabled;

    @NotNull
    private Boolean pushNotificationsEnabled;

    @NotNull
    private Boolean smsNotificationsEnabled;

    @NotNull
    private Boolean adminAlertsEnabled;

    @NotNull
    private Boolean userOrderNotificationsEnabled;

    @NotNull
    private Boolean userPromotionNotificationsEnabled;

    @NotNull
    private Boolean userSecurityNotificationsEnabled;
}

