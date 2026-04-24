package com.example.demo_project_spring_boot.dto;

import lombok.Data;


    @Data
    public class ReviewRequestDto {
        private Long productId;
        private Long userId;
        private Integer rating;
        private String comment;
    }
