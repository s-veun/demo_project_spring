package com.example.demo_project_spring_boot.dto.admin.settings;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EmailSettingsDto {

    @NotBlank
    private String smtpHost;

    @NotNull
    @Min(1)
    @Max(65535)
    private Integer smtpPort;

    @NotBlank
    private String smtpUsername;

    @NotBlank
    private String smtpPassword;

    @NotBlank
    @Email
    private String senderEmail;

    @NotBlank
    private String senderName;

    @NotNull
    private Boolean emailVerificationEnabled;

    @NotBlank
    private String forgotPasswordTemplate;

    @NotNull
    private Boolean tlsEnabled;
}

