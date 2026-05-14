package com.example.demo_project_spring_boot.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BannerRequestDto {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
    private String title;

    @Size(max = 500, message = "Subtitle must not exceed 500 characters")
    private String subtitle;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotBlank(message = "Button text is required")
    @Size(min = 2, max = 100, message = "Button text must be between 2 and 100 characters")
    private String buttonText;

    @NotBlank(message = "Button link is required")
    @Size(min = 5, max = 500, message = "Button link must be between 5 and 500 characters")
    private String buttonLink;

    private String imageUrl; // For image uploaded to cloud or local storage

    @NotBlank(message = "Background color is required")
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Background color must be a valid hex color code")
    private String backgroundColor;

    @NotBlank(message = "Text color is required")
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Text color must be a valid hex color code")
    private String textColor;

    @Size(max = 50, message = "Badge text must not exceed 50 characters")
    private String badgeText;

    @NotNull(message = "Position order is required")
    @Min(value = 0, message = "Position order must be non-negative")
    @Max(value = 999, message = "Position order must not exceed 999")
    private Integer positionOrder;

    @Builder.Default
    private Boolean isActive = true;

    // Image file upload - handled separately by multipart form
    private MultipartFile imageFile;
}

