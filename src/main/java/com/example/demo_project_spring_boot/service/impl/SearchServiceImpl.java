package com.example.demo_project_spring_boot.service.impl;

import com.example.demo_project_spring_boot.model.Product;
import com.example.demo_project_spring_boot.repository.ProductReposity;
import com.example.demo_project_spring_boot.service.SearchService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final ProductReposity productReposity;

    @Override
    public Page<Product> searchProducts(
            String keyword,
            Long categoryId,
            String brand,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean available,
            Double minRating,
            String sortBy,
            String sortDirection,
            Pageable pageable) {

        Specification<Product> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Keyword search (name, description, brand, tags)
            if (keyword != null && !keyword.isEmpty()) {
                String searchPattern = "%" + keyword.toLowerCase() + "%";
                Predicate namePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("proName")), searchPattern);
                Predicate descPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("proDesc")), searchPattern);
                Predicate brandPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("proBrand")), searchPattern);
                Predicate tagsPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("tags")), searchPattern);
                
                predicates.add(criteriaBuilder.or(namePredicate, descPredicate, brandPredicate, tagsPredicate));
            }

            // Category filter
            if (categoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("catId"), categoryId));
            }

            // Brand filter
            if (brand != null && !brand.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("proBrand"), brand));
            }

            // Price range
            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("proPrice"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("proPrice"), maxPrice));
            }

            // Availability
            if (available != null) {
                predicates.add(criteriaBuilder.equal(root.get("available"), available));
            }

            // Minimum rating
            if (minRating != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("rating"), minRating));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        // Sorting
        if (sortBy != null && !sortBy.isEmpty()) {
            Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) 
                    ? Sort.Direction.DESC : Sort.Direction.ASC;
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), 
                    Sort.by(direction, sortBy));
        }

        return productReposity.findAll(spec, pageable);
    }

    @Override
    public List<String> getSearchSuggestions(String query, int limit) {
        if (query == null || query.isEmpty()) {
            return Collections.emptyList();
        }

        List<Product> products = productReposity.findAll();
        String searchPattern = query.toLowerCase();

        return products.stream()
                .flatMap(p -> {
                    Set<String> suggestions = new HashSet<>();
                    if (p.getProName() != null && p.getProName().toLowerCase().contains(searchPattern)) {
                        suggestions.add(p.getProName());
                    }
                    if (p.getProBrand() != null && p.getProBrand().toLowerCase().contains(searchPattern)) {
                        suggestions.add(p.getProBrand());
                    }
                    if (p.getTags() != null) {
                        String[] tags = p.getTags().split(",");
                        for (String tag : tags) {
                            if (tag.toLowerCase().contains(searchPattern)) {
                                suggestions.add(tag.trim());
                            }
                        }
                    }
                    return suggestions.stream();
                })
                .distinct()
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getSearchFilters(String keyword, Long categoryId) {
        List<Product> products = productReposity.findAll();

        // Apply basic filters
        if (keyword != null || categoryId != null) {
            final String finalKeyword = keyword;
            final Long finalCategoryId = categoryId;
            
            products = products.stream()
                    .filter(p -> {
                        boolean matches = true;
                        
                        if (finalKeyword != null) {
                            String pattern = finalKeyword.toLowerCase();
                            matches = (p.getProName() != null && p.getProName().toLowerCase().contains(pattern)) ||
                                    (p.getProBrand() != null && p.getProBrand().toLowerCase().contains(pattern));
                        }
                        
                        if (finalCategoryId != null && p.getCategory() != null) {
                            matches = matches && p.getCategory().getCatId().equals(finalCategoryId);
                        }
                        
                        return matches;
                    })
                    .collect(Collectors.toList());
        }

        // Extract unique brands
        Set<String> brands = products.stream()
                .map(Product::getProBrand)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Price range
        Optional<BigDecimal> minPrice = products.stream()
                .map(Product::getProPrice)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo);
        
        Optional<BigDecimal> maxPrice = products.stream()
                .map(Product::getProPrice)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo);

        Map<String, Object> filters = new HashMap<>();
        filters.put("brands", brands);
        filters.put("minPrice", minPrice.orElse(BigDecimal.ZERO));
        filters.put("maxPrice", maxPrice.orElse(BigDecimal.ZERO));
        filters.put("totalProducts", products.size());

        return filters;
    }

    @Override
    public List<Product> searchByTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptyList();
        }

        return productReposity.findAll().stream()
                .filter(p -> p.getTags() != null)
                .filter(p -> {
                    String[] productTags = p.getTags().toLowerCase().split(",");
                    return tags.stream().anyMatch(tag -> 
                            Arrays.asList(productTags).contains(tag.toLowerCase().trim()));
                })
                .collect(Collectors.toList());
    }

    @Override
    public void saveSearchHistory(Long userId, String searchQuery) {
        // TODO: Implement with Redis or database storage
        // For now, this is a placeholder
    }

    @Override
    public List<String> getRecentSearches(Long userId, int limit) {
        // TODO: Implement with Redis or database storage
        return Collections.emptyList();
    }
}
