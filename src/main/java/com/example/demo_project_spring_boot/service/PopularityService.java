package com.example.demo_project_spring_boot.service;

import com.example.demo_project_spring_boot.dto.ProductResponseDTO;

import java.util.List;
import java.util.Map;

public interface PopularityService {

    // Track product view
    void trackProductView(Long productId, String sessionId, String ipAddress, String userAgent);

    // Increment purchase count
    void incrementPurchaseCount(Long productId);

    // Get popular products by different metrics
    List<ProductResponseDTO> getMostViewedProducts(int limit);

    List<ProductResponseDTO> getMostPurchasedProducts(int limit);

    List<ProductResponseDTO> getTopRatedProducts(int limit);

    List<ProductResponseDTO> getTrendingProducts(int limit); // Last 7 days

    // Get popularity score for a product
    Double calculatePopularityScore(Long productId);

    // Update all popularity scores (scheduled task)
    void updateAllPopularityScores();

    // Get product analytics
    Map<String, Object> getProductAnalytics(Long productId);

    // Get recently viewed products for user
    List<ProductResponseDTO> getRecentlyViewedByUser(Long userId, int limit);

    // Get recommended products based on viewing history
    List<ProductResponseDTO> getRecommendedProducts(Long userId, int limit);
}