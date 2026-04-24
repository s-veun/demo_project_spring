package com.example.demo_project_spring_boot.repository;

import com.example.demo_project_spring_boot.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProduct_ProId(Long productId);
    boolean existsByUser_IdAndProduct_ProId(Long userId, Long productId);
}
