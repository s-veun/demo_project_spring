package com.example.demo_project_spring_boot.dto.admin.settings;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SeoSettingsDto {

    @NotBlank
    private String metaTitle;

    @NotBlank
    private String metaDescription;

    @NotBlank
    private String metaKeywords;

    private String openGraphTitle;

    private String openGraphDescription;

    private String openGraphImageUrl;

    private String googleAnalyticsId;

    private String facebookPixelId;
}

