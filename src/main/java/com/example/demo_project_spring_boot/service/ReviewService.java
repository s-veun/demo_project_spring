package com.example.demo_project_spring_boot.service;

import com.example.demo_project_spring_boot.dto.ReviewRequestDto;
import com.example.demo_project_spring_boot.dto.ReviewResponseDto;

import java.util.List;

public interface ReviewService {
    ReviewResponseDto addReview(ReviewRequestDto requestDto);
    List<ReviewResponseDto> getReviewsByProduct(Long productId);
    void deleteReview(Long reviewId);
}
