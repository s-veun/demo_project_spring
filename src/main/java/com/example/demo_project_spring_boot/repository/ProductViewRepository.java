package com.example.demo_project_spring_boot.repository;

import com.example.demo_project_spring_boot.model.ProductView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProductViewRepository extends JpaRepository<ProductView, Long> {
    
    // Find views by product
    List<ProductView> findByProductProId(Long productId);
    
    // Find views by user
    List<ProductView> findByUserId(Long userId);
    
    // Count views by product in time period
    @Query("SELECT COUNT(pv) FROM ProductView pv WHERE pv.product.proId = :productId AND pv.viewedAt >= :startDate")
    Long countViewsByProductSince(@Param("productId") Long productId, @Param("startDate") LocalDateTime startDate);
    
    // Find unique viewers for a product
    @Query("SELECT COUNT(DISTINCT pv.sessionId) FROM ProductView pv WHERE pv.product.proId = :productId")
    Long countUniqueViewersByProduct(@Param("productId") Long productId);
    
    // Get most viewed products in time period
    @Query("SELECT pv.product.proId, COUNT(pv) as viewCount FROM ProductView pv " +
           "WHERE pv.viewedAt >= :startDate " +
           "GROUP BY pv.product.proId ORDER BY viewCount DESC")
    List<Object[]> findMostViewedProducts(@Param("startDate") LocalDateTime startDate);
    
    // Get recent views for recommendations
    @Query("SELECT pv.product FROM ProductView pv WHERE pv.user.id = :userId ORDER BY pv.viewedAt DESC")
    List<Object> findRecentlyViewedByUser(@Param("userId") Long userId);
    
    // Delete old views (cleanup)
    void deleteByViewedAtBefore(LocalDateTime date);
}
