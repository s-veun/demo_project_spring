package com.example.demo_project_spring_boot.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewRequestDto {
    @NotNull(message = "Product ID is required")
    private Long productId;

    // userId is set by the server from the JWT token — field kept for internal use only
    private Long userId;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    private Integer rating;

    private String comment;
}
