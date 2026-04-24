package com.example.demo_project_spring_boot.repository;

import com.example.demo_project_spring_boot.model.ReviewVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewVoteRepository extends JpaRepository<ReviewVote, Long> {
    
    // Check if user already voted on review
    Optional<ReviewVote> findByReviewReviewIdAndUserId(Long reviewId, Long userId);
    
    // Count helpful votes for a review
    @Query("SELECT COUNT(rv) FROM ReviewVote rv WHERE rv.review.reviewId = :reviewId AND rv.isHelpful = true")
    Long countHelpfulVotes(@Param("reviewId") Long reviewId);
    
    // Count not helpful votes
    @Query("SELECT COUNT(rv) FROM ReviewVote rv WHERE rv.review.reviewId = :reviewId AND rv.isHelpful = false")
    Long countNotHelpfulVotes(@Param("reviewId") Long reviewId);
    
    // Get all votes by user
    List<ReviewVote> findByUserId(Long userId);
    
    // Delete vote
    void deleteByReviewReviewIdAndUserId(Long reviewId, Long userId);
}
