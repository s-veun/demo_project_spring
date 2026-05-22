package com.example.demo_project_spring_boot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "app.settings.upload")
public class SettingsUploadProperties {

    private long maxFileSizeBytes = 2_097_152;
    private List<String> allowedContentTypes = List.of("image/png", "image/jpeg", "image/svg+xml", "image/x-icon", "image/vnd.microsoft.icon");
    private String cloudinaryFolder = "settings/assets";
}

