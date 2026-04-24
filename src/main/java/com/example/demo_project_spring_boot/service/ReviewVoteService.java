package com.example.demo_project_spring_boot.service;

import java.util.Map;

public interface ReviewVoteService {
    
    // Vote on a review
    Map<String, Object> voteReview(Long reviewId, Long userId, boolean isHelpful);
    
    // Get vote statistics for a review
    Map<String, Object> getReviewVotes(Long reviewId);
    
    // Check if user has voted
    boolean hasUserVoted(Long reviewId, Long userId);
    
    // Remove vote
    void removeVote(Long reviewId, Long userId);
}
