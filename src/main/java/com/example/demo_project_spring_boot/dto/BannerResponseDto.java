package com.example.demo_project_spring_boot.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BannerResponseDto {
    private Long bannerId;
    private String title;
    private String subtitle;
    private String description;
    private String buttonText;
    private String buttonLink;
    private String imageUrl;
    private String backgroundColor;
    private String textColor;
    private String badgeText;
    private Integer positionOrder;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

