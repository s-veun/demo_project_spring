package com.example.demo_project_spring_boot.service;

import com.example.demo_project_spring_boot.model.Product;

import java.util.List;
import java.util.Map;

public interface PopularityService {
    
    // Track product view
    void trackProductView(Long productId, String sessionId, String ipAddress, String userAgent);
    
    // Increment purchase count
    void incrementPurchaseCount(Long productId);
    
    // Get popular products by different metrics
    List<Product> getMostViewedProducts(int limit);
    List<Product> getMostPurchasedProducts(int limit);
    List<Product> getTopRatedProducts(int limit);
    List<Product> getTrendingProducts(int limit); // Last 7 days
    
    // Get popularity score for a product
    Double calculatePopularityScore(Long productId);
    
    // Update all popularity scores (scheduled task)
    void updateAllPopularityScores();
    
    // Get product analytics
    Map<String, Object> getProductAnalytics(Long productId);
    
    // Get recently viewed products for user
    List<Product> getRecentlyViewedByUser(Long userId, int limit);
    
    // Get recommended products based on viewing history
    List<Product> getRecommendedProducts(Long userId, int limit);
}
