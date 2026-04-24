package com.example.demo_project_spring_boot.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewResponseDto {
    private Long reviewId;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
    private String username;
}
