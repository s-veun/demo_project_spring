package com.example.demo_project_spring_boot.service;

import com.example.demo_project_spring_boot.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface SearchService {
    
    // Full-text search with filters
    Page<Product> searchProducts(
            String keyword,
            Long categoryId,
            String brand,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean available,
            Double minRating,
            String sortBy,
            String sortDirection,
            Pageable pageable
    );
    
    // Search suggestions (autocomplete)
    List<String> getSearchSuggestions(String query, int limit);
    
    // Get available filters for current search
    Map<String, Object> getSearchFilters(String keyword, Long categoryId);
    
    // Search by tags
    List<Product> searchByTags(List<String> tags);
    
    // Recently searched (can be stored in Redis later)
    void saveSearchHistory(Long userId, String searchQuery);
    List<String> getRecentSearches(Long userId, int limit);
}
