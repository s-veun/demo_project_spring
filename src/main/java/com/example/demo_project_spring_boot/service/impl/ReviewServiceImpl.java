package com.example.demo_project_spring_boot.service.impl;

import com.example.demo_project_spring_boot.dto.ReviewRequestDto;
import com.example.demo_project_spring_boot.dto.ReviewResponseDto;
import com.example.demo_project_spring_boot.model.Product;
import com.example.demo_project_spring_boot.model.Review;
import com.example.demo_project_spring_boot.model.User;
import com.example.demo_project_spring_boot.repository.ProductReposity; // ប្រើឈ្មោះកូដដើមរបស់អ្នក
import com.example.demo_project_spring_boot.repository.ReviewRepository;
import com.example.demo_project_spring_boot.repository.UserRepository;
import com.example.demo_project_spring_boot.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ProductReposity productRepository;

    @Override
    public ReviewResponseDto addReview(ReviewRequestDto requestDto) {
        // ១. ពិនិត្យមើលពិន្ទុ (Rating Validation)
        if (requestDto.getRating() == null || requestDto.getRating() < 1 || requestDto.getRating() > 5) {
            throw new RuntimeException("Rating must be between 1 and 5 stars!");
        }

        // ២. ការពារកុំឱ្យ User ម្នាក់ Review លើ Product មួយដដែលៗ (Anti-Spam)
        if (reviewRepository.existsByUser_IdAndProduct_ProId(requestDto.getUserId(), requestDto.getProductId())) {
            throw new RuntimeException("You have already reviewed this product!");
        }

        // ៣. ស្វែងរក User និង Product ក្នុង Database
        User user = userRepository.findById(requestDto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + requestDto.getUserId()));

        Product product = productRepository.findById(requestDto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + requestDto.getProductId()));

        // ៤. បង្កើត Review ថ្មី និងរក្សាទុក
        Review review = new Review();
        review.setRating(requestDto.getRating());
        review.setComment(requestDto.getComment());
        review.setUser(user);
        review.setProduct(product);
        review.setCreatedAt(LocalDateTime.now());

        Review savedReview = reviewRepository.save(review);

        // ៥. បំប្លែង Entity ទៅជា DTO វិញ
        return mapToDto(savedReview);
    }

    @Override
    public List<ReviewResponseDto> getReviewsByProduct(Long productId) {
        // ទាញយក Review ទាំងអស់របស់ Product នោះ រួចបំប្លែងទៅជា DTO List
        return reviewRepository.findByProduct_ProId(productId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteReview(Long reviewId) {
        // ឆែកមើលថាតើ Review នោះមានពិតមែនឬអត់ មុននឹងលុប
        if (!reviewRepository.existsById(reviewId)) {
            throw new RuntimeException("Review not found with ID: " + reviewId);
        }
        reviewRepository.deleteById(reviewId);
    }

    // ====================================================
    // Helper Method សម្រាប់បំប្លែង Review (Entity) ទៅ ReviewResponseDto
    // ====================================================
    private ReviewResponseDto mapToDto(Review review) {
        ReviewResponseDto dto = new ReviewResponseDto();
        dto.setReviewId(review.getReviewId());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setCreatedAt(review.getCreatedAt());

        // បង្ហាញតែឈ្មោះ Username បានហើយ ដើម្បីសុវត្ថិភាពទិន្នន័យ (កុំបោះ User ទាំងមូល)
        if (review.getUser() != null) {
            dto.setUsername(review.getUser().getUsername());
        }

        return dto;
    }
}