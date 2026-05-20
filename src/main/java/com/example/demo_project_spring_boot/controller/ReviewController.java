package com.example.demo_project_spring_boot.controller;

import com.example.demo_project_spring_boot.dto.ReviewRequestDto;
import com.example.demo_project_spring_boot.dto.ReviewResponseDto;
import com.example.demo_project_spring_boot.exception.UnauthorizedException;
import com.example.demo_project_spring_boot.model.User;
import com.example.demo_project_spring_boot.repository.UserRepository;
import com.example.demo_project_spring_boot.service.ReviewService;
import com.example.demo_project_spring_boot.service.ReviewVoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reviews", description = "Product review management APIs")
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewVoteService reviewVoteService;
    private final UserRepository userRepository;

    /** Add a review — requires authentication; userId is taken from JWT, not request body */
    @PostMapping("/add")
    @Transactional
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Add a product review (authenticated users only)")
    public ResponseEntity<?> addReview(
            @RequestBody @Valid ReviewRequestDto requestDto,
            Authentication authentication) {
        try {
            Long userId = resolveUserId(authentication);
            // Override any userId in the request body with the authenticated user's ID
            requestDto.setUserId(userId);
            ReviewResponseDto response = reviewService.addReview(requestDto);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Get all reviews for a product — public */
    @GetMapping("/product/{productId}")
    @Transactional
    @Operation(summary = "Get all reviews for a product")
    public ResponseEntity<List<ReviewResponseDto>> getReviewsByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getReviewsByProduct(productId));
    }

    /** Delete a review — requires authentication (owner or admin) */
    @DeleteMapping("/{reviewId}")
    @Transactional
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete a review (owner or admin only)")
    public ResponseEntity<?> deleteReview(@PathVariable Long reviewId) {
        try {
            reviewService.deleteReview(reviewId);
            return ResponseEntity.ok(Map.of("message", "Review deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Vote on a review — requires authentication */
    @PostMapping("/{reviewId}/vote")
    @Transactional
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Vote on a review (helpful or not)")
    public ResponseEntity<?> voteReview(
            @PathVariable Long reviewId,
            @RequestParam boolean helpful,
            Authentication authentication) {
        try {
            Long userId = resolveUserId(authentication);
            Map<String, Object> result = reviewVoteService.voteReview(reviewId, userId, helpful);
            return ResponseEntity.ok(result);
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Get review vote statistics — public */
    @GetMapping("/{reviewId}/votes")
    @Operation(summary = "Get vote statistics for a review")
    public ResponseEntity<?> getReviewVotes(@PathVariable Long reviewId) {
        try {
            Map<String, Object> stats = reviewVoteService.getReviewVotes(reviewId);
            return ResponseEntity.ok(stats);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Remove own vote from a review — requires authentication */
    @DeleteMapping("/{reviewId}/vote")
    @Transactional
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Remove own vote from a review")
    public ResponseEntity<?> removeVote(
            @PathVariable Long reviewId,
            Authentication authentication) {
        try {
            Long userId = resolveUserId(authentication);
            reviewVoteService.removeVote(reviewId, userId);
            return ResponseEntity.ok(Map.of("message", "Vote removed"));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Long resolveUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            throw new UnauthorizedException("Authentication required");
        }
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new UnauthorizedException("Authenticated user not found in database"));
        return user.getId();
    }
}
