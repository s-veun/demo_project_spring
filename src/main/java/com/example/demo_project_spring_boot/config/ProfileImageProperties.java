package com.example.demo_project_spring_boot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.profile-image")
public class ProfileImageProperties {

    private String storageProvider = "cloudinary";
    private String uploadDir = "uploads/profile-images";
    private String publicBasePath = "/uploads/profile-images";
    private String cloudinaryFolder = "uploads/profile-images";
    private String defaultUrl = "/uploads/profile-images/default-avatar.png";
    private String apiBaseUrl = "";
    private long maxFileSizeBytes = 5L * 1024L * 1024L;
    private List<String> allowedExtensions = List.of("jpg", "jpeg", "png", "webp");
    private List<String> allowedMimeTypes = List.of("image/jpeg", "image/png", "image/webp");
}

