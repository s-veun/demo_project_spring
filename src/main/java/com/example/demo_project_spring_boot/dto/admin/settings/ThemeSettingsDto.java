package com.example.demo_project_spring_boot.dto.admin.settings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ThemeSettingsDto {

    @NotBlank
    @Pattern(regexp = "^#[A-Fa-f0-9]{6}$", message = "Primary color must be in hex format")
    private String primaryColor;

    @NotBlank
    @Pattern(regexp = "^#[A-Fa-f0-9]{6}$", message = "Secondary color must be in hex format")
    private String secondaryColor;

    @NotBlank
    private String mode;

    @NotBlank
    private String sidebarStyle;

    @NotBlank
    private String dashboardLayout;

    private String logoUrl;

    private String faviconUrl;
}

