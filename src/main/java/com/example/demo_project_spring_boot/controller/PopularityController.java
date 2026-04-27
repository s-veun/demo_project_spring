package com.example.demo_project_spring_boot.controller;

import com.example.demo_project_spring_boot.dto.ProductResponseDTO;
import com.example.demo_project_spring_boot.service.PopularityService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/popularity")
@RequiredArgsConstructor
public class PopularityController {

    private final PopularityService popularityService;

    @PostMapping("/view/{productId}")
    public ResponseEntity<?> trackView(@PathVariable Long productId, HttpServletRequest request) {
        String sessionId = request.getSession().getId();
        String ipAddress = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        popularityService.trackProductView(productId, sessionId, ipAddress, userAgent);

        return ResponseEntity.ok(Map.of("message", "View tracked"));
    }

    @GetMapping("/most-viewed")
    public ResponseEntity<?> getMostViewed(
            @RequestParam(defaultValue = "10") int limit) {
        List<ProductResponseDTO> products = popularityService.getMostViewedProducts(limit);
        return ResponseEntity.ok(Map.of(
                "count", products.size(),
                "products", products
        ));
    }

    @GetMapping("/most-purchased")
    public ResponseEntity<?> getMostPurchased(
            @RequestParam(defaultValue = "10") int limit) {
        List<ProductResponseDTO> products = popularityService.getMostPurchasedProducts(limit);
        return ResponseEntity.ok(Map.of(
                "count", products.size(),
                "products", products
        ));
    }

    @GetMapping("/top-rated")
    public ResponseEntity<?> getTopRated(
            @RequestParam(defaultValue = "10") int limit) {
        List<ProductResponseDTO> products = popularityService.getTopRatedProducts(limit);
        return ResponseEntity.ok(Map.of(
                "count", products.size(),
                "products", products
        ));
    }

    @GetMapping("/trending")
    public ResponseEntity<?> getTrending(
            @RequestParam(defaultValue = "10") int limit) {
        List<ProductResponseDTO> products = popularityService.getTrendingProducts(limit);
        return ResponseEntity.ok(Map.of(
                "count", products.size(),
                "products", products
        ));
    }

    @GetMapping("/product/{productId}/analytics")
    public ResponseEntity<?> getProductAnalytics(@PathVariable Long productId) {
        Map<String, Object> analytics = popularityService.getProductAnalytics(productId);
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/product/{productId}/score")
    public ResponseEntity<?> getPopularityScore(@PathVariable Long productId) {
        Double score = popularityService.calculatePopularityScore(productId);
        return ResponseEntity.ok(Map.of(
                "productId", productId,
                "popularityScore", score
        ));
    }

    @GetMapping("/user/recently-viewed")
    public ResponseEntity<?> getRecentlyViewed(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        List<ProductResponseDTO> products = popularityService.getRecentlyViewedByUser(userId, limit);
        return ResponseEntity.ok(Map.of(
                "count", products.size(),
                "products", products
        ));
    }

    @GetMapping("/user/recommendations")
    public ResponseEntity<?> getRecommendations(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        List<ProductResponseDTO> products = popularityService.getRecommendedProducts(userId, limit);
        return ResponseEntity.ok(Map.of(
                "count", products.size(),
                "products", products
        ));
    }

    @PostMapping("/update-all-scores")
    public ResponseEntity<?> updateAllScores() {
        popularityService.updateAllPopularityScores();
        return ResponseEntity.ok(Map.of("message", "All popularity scores updated"));
    }
}
