package com.example.demo_project_spring_boot.controller;

import com.example.demo_project_spring_boot.model.Product;
import com.example.demo_project_spring_boot.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/products")
    public ResponseEntity<?> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) Double minRating,
            @RequestParam(defaultValue = "popularityScore") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);

        Page<Product> products = searchService.searchProducts(
                keyword, categoryId, brand, minPrice, maxPrice,
                available, minRating, sortBy, sortDirection, pageable);

        return ResponseEntity.ok(Map.of(
                "products", products.getContent(),
                "currentPage", products.getNumber(),
                "totalItems", products.getTotalElements(),
                "totalPages", products.getTotalPages(),
                "pageSize", products.getSize()
        ));
    }

    @GetMapping("/suggestions")
    public ResponseEntity<?> getSearchSuggestions(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {
        List<String> suggestions = searchService.getSearchSuggestions(query, limit);
        return ResponseEntity.ok(Map.of("suggestions", suggestions));
    }

    @GetMapping("/filters")
    public ResponseEntity<?> getSearchFilters(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId) {
        Map<String, Object> filters = searchService.getSearchFilters(keyword, categoryId);
        return ResponseEntity.ok(filters);
    }

    @GetMapping("/by-tags")
    public ResponseEntity<?> searchByTags(@RequestParam List<String> tags) {
        List<Product> products = searchService.searchByTags(tags);
        return ResponseEntity.ok(Map.of(
                "count", products.size(),
                "products", products
        ));
    }
}
