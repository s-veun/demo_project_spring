package com.example.demo_project_spring_boot.service.impl;

import com.example.demo_project_spring_boot.exception.ResourceNotFoundException;
import com.example.demo_project_spring_boot.model.Product;
import com.example.demo_project_spring_boot.model.ProductView;
import com.example.demo_project_spring_boot.model.User;
import com.example.demo_project_spring_boot.repository.ProductReposity;
import com.example.demo_project_spring_boot.repository.ProductViewRepository;
import com.example.demo_project_spring_boot.repository.UserRepository;
import com.example.demo_project_spring_boot.service.PopularityService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class PopularityServiceImpl implements PopularityService {

    private final ProductViewRepository productViewRepository;
    private final ProductReposity productReposity;
    private final UserRepository userRepository;

    @Override
    public void trackProductView(Long productId, String sessionId, String ipAddress, String userAgent) {
        Product product = productReposity.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        ProductView view = ProductView.builder()
                .product(product)
                .sessionId(sessionId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        productViewRepository.save(view);

        // Increment view count
        product.setViewCount(product.getViewCount() + 1);
        productReposity.save(product);
    }

    @Override
    public void incrementPurchaseCount(Long productId) {
        Product product = productReposity.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        product.setPurchaseCount(product.getPurchaseCount() + 1);
        productReposity.save(product);
    }

    @Override
    public List<Product> getMostViewedProducts(int limit) {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<Object[]> results = productViewRepository.findMostViewedProducts(sevenDaysAgo);
        
        return results.stream()
                .limit(limit)
                .map(row -> (Long) row[0])
                .map(productId -> productReposity.findById(productId).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<Product> getMostPurchasedProducts(int limit) {
        return productReposity.findAll().stream()
                .sorted(Comparator.comparing(Product::getPurchaseCount).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<Product> getTopRatedProducts(int limit) {
        return productReposity.findAll().stream()
                .filter(p -> p.getRating() != null)
                .sorted(Comparator.comparing(Product::getRating).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<Product> getTrendingProducts(int limit) {
        // Products with most views in last 7 days that are above average
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<Object[]> results = productViewRepository.findMostViewedProducts(sevenDaysAgo);
        
        if (results.isEmpty()) {
            return Collections.emptyList();
        }

        double averageViews = results.stream()
                .mapToDouble(row -> (Long) row[1])
                .average()
                .orElse(0);

        return results.stream()
                .filter(row -> (Long) row[1] > averageViews)
                .limit(limit)
                .map(row -> (Long) row[0])
                .map(productId -> productReposity.findById(productId).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public Double calculatePopularityScore(Long productId) {
        Product product = productReposity.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Popularity algorithm:
        // Score = (viewCount * 0.3) + (purchaseCount * 0.5) + (rating * 20 * 0.2)
        double viewScore = (product.getViewCount() != null ? product.getViewCount() : 0) * 0.3;
        double purchaseScore = (product.getPurchaseCount() != null ? product.getPurchaseCount() : 0) * 0.5;
        double ratingScore = (product.getRating() != null ? product.getRating() : 0) * 20 * 0.2;

        double score = viewScore + purchaseScore + ratingScore;
        
        // Update product with calculated score
        product.setPopularityScore(score);
        productReposity.save(product);

        return score;
    }

    @Override
    public void updateAllPopularityScores() {
        List<Product> allProducts = productReposity.findAll();
        for (Product product : allProducts) {
            calculatePopularityScore(product.getProId());
        }
    }

    @Override
    public Map<String, Object> getProductAnalytics(Long productId) {
        Product product = productReposity.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        Map<String, Object> analytics = new HashMap<>();
        analytics.put("totalViews", product.getViewCount());
        analytics.put("totalPurchases", product.getPurchaseCount());
        analytics.put("rating", product.getRating());
        analytics.put("popularityScore", product.getPopularityScore());
        
        // Views in last 7 days
        analytics.put("viewsLast7Days", 
                productViewRepository.countViewsByProductSince(productId, sevenDaysAgo));
        
        // Views in last 30 days
        analytics.put("viewsLast30Days", 
                productViewRepository.countViewsByProductSince(productId, thirtyDaysAgo));
        
        // Unique viewers
        analytics.put("uniqueViewers", 
                productViewRepository.countUniqueViewersByProduct(productId));

        return analytics;
    }

    @Override
    public List<Product> getRecentlyViewedByUser(Long userId, int limit) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Object> results = productViewRepository.findRecentlyViewedByUser(userId);
        
        return results.stream()
                .filter(obj -> obj instanceof Product)
                .map(obj -> (Product) obj)
                .distinct()
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<Product> getRecommendedProducts(Long userId, int limit) {
        // Get user's recently viewed products' categories
        List<Product> recentlyViewed = getRecentlyViewedByUser(userId, 20);
        
        if (recentlyViewed.isEmpty()) {
            // If no history, return trending products
            return getTrendingProducts(limit);
        }

        // Get categories from recently viewed
        Set<Long> categoryIds = recentlyViewed.stream()
                .map(p -> p.getCategory() != null ? p.getCategory().getCatId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Find similar products in same categories, excluding already viewed
        Set<Long> viewedIds = recentlyViewed.stream()
                .map(Product::getProId)
                .collect(Collectors.toSet());

        List<Product> recommendations = productReposity.findAll().stream()
                .filter(p -> p.getCategory() != null && categoryIds.contains(p.getCategory().getCatId()))
                .filter(p -> !viewedIds.contains(p.getProId()))
                .sorted(Comparator.comparing(Product::getPopularityScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .collect(Collectors.toList());

        // Fill remaining slots with trending products if needed
        if (recommendations.size() < limit) {
            List<Product> trending = getTrendingProducts(limit - recommendations.size());
            recommendations.addAll(trending.stream()
                    .filter(p -> !viewedIds.contains(p.getProId()))
                    .limit(limit - recommendations.size())
                    .collect(Collectors.toList()));
        }

        return recommendations;
    }
}
