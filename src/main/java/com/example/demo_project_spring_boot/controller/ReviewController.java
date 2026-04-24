package com.example.demo_project_spring_boot.controller;

import com.example.demo_project_spring_boot.dto.ReviewRequestDto;
import com.example.demo_project_spring_boot.dto.ReviewResponseDto;
import com.example.demo_project_spring_boot.service.ReviewService;
import com.example.demo_project_spring_boot.service.ReviewVoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewVoteService reviewVoteService;

    // ១. បន្ថែមការបញ្ចេញមតិ (Add Review)
    @PostMapping("/add")
    @Transactional
    public ResponseEntity<?> addReview(@RequestBody ReviewRequestDto requestDto) {
        try {
            ReviewResponseDto response = reviewService.addReview(requestDto);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ២. មើលបញ្ចេញមតិទាំងអស់របស់ផលិតផលណាមួយ (Get Reviews by Product)
    @GetMapping("/product/{productId}")
    @Transactional
    public ResponseEntity<List<ReviewResponseDto>> getReviewsByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getReviewsByProduct(productId));
    }

    // ៣. លុបការបញ្ចេញមតិ (Delete Review)
    @DeleteMapping("/{reviewId}")
    @Transactional
    public ResponseEntity<String> deleteReview(@PathVariable Long reviewId) {
        try {
            reviewService.deleteReview(reviewId);
            return ResponseEntity.ok("Review deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ៤. បោះឆ្នោតលើការបញ្ចេញមតិ (Vote on Review)
    @PostMapping("/{reviewId}/vote")
    @Transactional
    public ResponseEntity<?> voteReview(
            @PathVariable Long reviewId,
            @RequestParam Long userId,
            @RequestParam boolean helpful) {
        try {
            Map<String, Object> result = reviewVoteService.voteReview(reviewId, userId, helpful);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ៥. មើលស្ថិតិបោះឆ្នោត (Get Review Vote Stats)
    @GetMapping("/{reviewId}/votes")
    public ResponseEntity<?> getReviewVotes(@PathVariable Long reviewId) {
        try {
            Map<String, Object> stats = reviewVoteService.getReviewVotes(reviewId);
            return ResponseEntity.ok(stats);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ៦. ដកបោះឆ្នោតចេញ (Remove Vote)
    @DeleteMapping("/{reviewId}/vote")
    @Transactional
    public ResponseEntity<?> removeVote(
            @PathVariable Long reviewId,
            @RequestParam Long userId) {
        try {
            reviewVoteService.removeVote(reviewId, userId);
            return ResponseEntity.ok(Map.of("message", "Vote removed"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
