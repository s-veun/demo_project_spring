package com.example.demo_project_spring_boot.dto.admin.settings;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SocialSettingsDto {

    @Pattern(regexp = "^(https?://.*)?$", message = "Facebook URL must be a valid URL")
    private String facebookUrl;

    @Pattern(regexp = "^(https?://.*)?$", message = "Telegram URL must be a valid URL")
    private String telegramUrl;

    @Pattern(regexp = "^(https?://.*)?$", message = "Instagram URL must be a valid URL")
    private String instagramUrl;

    @Pattern(regexp = "^(https?://.*)?$", message = "TikTok URL must be a valid URL")
    private String tiktokUrl;

    @Pattern(regexp = "^(https?://.*)?$", message = "YouTube URL must be a valid URL")
    private String youtubeUrl;

    @Pattern(regexp = "^(https?://.*)?$", message = "LinkedIn URL must be a valid URL")
    private String linkedInUrl;
}

