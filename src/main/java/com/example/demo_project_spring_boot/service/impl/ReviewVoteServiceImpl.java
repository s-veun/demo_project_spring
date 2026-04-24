package com.example.demo_project_spring_boot.service.impl;

import com.example.demo_project_spring_boot.exception.ResourceNotFoundException;
import com.example.demo_project_spring_boot.model.Review;
import com.example.demo_project_spring_boot.model.ReviewVote;
import com.example.demo_project_spring_boot.model.User;
import com.example.demo_project_spring_boot.repository.ReviewRepository;
import com.example.demo_project_spring_boot.repository.ReviewVoteRepository;
import com.example.demo_project_spring_boot.repository.UserRepository;
import com.example.demo_project_spring_boot.service.ReviewVoteService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class ReviewVoteServiceImpl implements ReviewVoteService {

    private final ReviewVoteRepository reviewVoteRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    @Override
    public Map<String, Object> voteReview(Long reviewId, Long userId, boolean isHelpful) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check if already voted
        Map<String, Object> result = new HashMap<>();
        
        if (reviewVoteRepository.findByReviewReviewIdAndUserId(reviewId, userId).isPresent()) {
            // Update existing vote
            ReviewVote existingVote = reviewVoteRepository.findByReviewReviewIdAndUserId(reviewId, userId).get();
            existingVote.setIsHelpful(isHelpful);
            reviewVoteRepository.save(existingVote);
            result.put("action", "updated");
        } else {
            // Create new vote
            ReviewVote vote = ReviewVote.builder()
                    .review(review)
                    .user(user)
                    .isHelpful(isHelpful)
                    .build();
            reviewVoteRepository.save(vote);
            result.put("action", "created");
        }

        // Update review's helpful count
        Long helpfulCount = reviewVoteRepository.countHelpfulVotes(reviewId);
        review.setHelpfulVotes(helpfulCount.intValue());
        reviewRepository.save(review);

        result.put("reviewId", reviewId);
        result.put("helpful", isHelpful);
        result.put("helpfulCount", helpfulCount);
        result.put("message", isHelpful ? "Marked as helpful" : "Marked as not helpful");

        return result;
    }

    @Override
    public Map<String, Object> getReviewVotes(Long reviewId) {
        if (!reviewRepository.existsById(reviewId)) {
            throw new ResourceNotFoundException("Review not found");
        }

        Long helpful = reviewVoteRepository.countHelpfulVotes(reviewId);
        Long notHelpful = reviewVoteRepository.countNotHelpfulVotes(reviewId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("reviewId", reviewId);
        stats.put("helpful", helpful);
        stats.put("notHelpful", notHelpful);
        stats.put("total", helpful + notHelpful);
        stats.put("helpfulPercentage", (helpful + notHelpful) > 0 
                ? (helpful * 100.0 / (helpful + notHelpful)) : 0);

        return stats;
    }

    @Override
    public boolean hasUserVoted(Long reviewId, Long userId) {
        return reviewVoteRepository.findByReviewReviewIdAndUserId(reviewId, userId).isPresent();
    }

    @Override
    public void removeVote(Long reviewId, Long userId) {
        if (!reviewVoteRepository.findByReviewReviewIdAndUserId(reviewId, userId).isPresent()) {
            throw new ResourceNotFoundException("Vote not found");
        }
        
        reviewVoteRepository.deleteByReviewReviewIdAndUserId(reviewId, userId);
        
        // Update review's helpful count
        Long helpfulCount = reviewVoteRepository.countHelpfulVotes(reviewId);
        Review review = reviewRepository.findById(reviewId).orElseThrow();
        review.setHelpfulVotes(helpfulCount.intValue());
        reviewRepository.save(review);
    }
}
