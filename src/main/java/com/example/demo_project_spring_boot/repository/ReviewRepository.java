package com.example.demo_project_spring_boot.repository;

import com.example.demo_project_spring_boot.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProduct_ProId(Long productId);
    boolean existsByUser_IdAndProduct_ProId(Long userId, Long productId);
    long countByUser_Id(Long userId);

    @Query("""
            SELECT r FROM reviews r
            LEFT JOIN r.user u
            LEFT JOIN r.product p
            WHERE (:keyword IS NULL OR :keyword = ''
                   OR LOWER(COALESCE(r.comment, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(COALESCE(u.username, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(COALESCE(p.proName, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:rating IS NULL OR r.rating = :rating)
            """)
    Page<Review> searchForAdmin(@Param("keyword") String keyword,
                                @Param("rating") Integer rating,
                                Pageable pageable);

    long countByRating(Integer rating);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM reviews r")
    Double averageRating();
}
