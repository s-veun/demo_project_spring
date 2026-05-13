package com.example.demo_project_spring_boot.service.impl;

import com.example.demo_project_spring_boot.dto.CategoryDTO;
import com.example.demo_project_spring_boot.exception.DuplicateResourceException;
import com.example.demo_project_spring_boot.exception.ResourceNotFoundException;
import com.example.demo_project_spring_boot.model.Category;
import com.example.demo_project_spring_boot.repository.CategoryRepository;
import com.example.demo_project_spring_boot.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    // 🛠 មុខងារជំនួយទី១៖ បំប្លែងពី Entity ទៅជា DTO (សម្រាប់បញ្ជូនចេញទៅ Postman)
    private CategoryDTO mapToDTO(Category category) {
        return new CategoryDTO(
                category.getCatId(),
                category.getCatName(),
                category.getSlug(),
                category.getCatDesc(),
                category.getImageUrl(),
                category.getIsActive(),
                category.getSortOrder()
        );
    }

    private String normalizeSlug(String source) {
        if (source == null) return null;
        return source.toLowerCase().trim().replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
    }

    // 🛠 មុខងារជំនួយទី២៖ បំប្លែងពី DTO ទៅជា Entity (សម្រាប់ Save ចូល Database)
    private Category mapToEntity(CategoryDTO dto) {
        Category category = new Category();
        category.setCatName(dto.getCatName());
        category.setSlug(dto.getSlug() == null || dto.getSlug().isBlank() ? normalizeSlug(dto.getCatName()) : normalizeSlug(dto.getSlug()));
        category.setCatDesc(dto.getCatDesc());
        category.setImageUrl(dto.getImageUrl());
        category.setIsActive(dto.getIsActive() == null ? Boolean.TRUE : dto.getIsActive());
        category.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        return category;
    }

    @Override
    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        // ឆែកមើលក្រែងលោមានឈ្មោះជាន់គ្នា
        if (categoryRepository.existsByCatName(categoryDTO.getCatName())) {
            throw new DuplicateResourceException("ឈ្មោះប្រភេទ '" + categoryDTO.getCatName() + "' មានរួចហើយ!");
        }

        String slug = categoryDTO.getSlug() == null || categoryDTO.getSlug().isBlank()
                ? normalizeSlug(categoryDTO.getCatName())
                : normalizeSlug(categoryDTO.getSlug());

        if (slug != null && categoryRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Slug '" + slug + "' មានរួចហើយ!");
        }

        // ១. បំប្លែង DTO ទៅ Entity
        Category category = mapToEntity(categoryDTO);

        // ២. Save ចូល Database
        Category savedCategory = categoryRepository.save(category);

        // ៣. បំប្លែង Entity ដែល Save រួច ទៅជា DTO វិញ រួចបញ្ជូនចេញ
        return mapToDTO(savedCategory);
    }

    @Override
    public List<CategoryDTO> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        // ទាញទិន្នន័យទាំងអស់មក ហើយបំប្លែងវាទៅជា DTO មួយៗ
        return categories.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryDTO getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("រកមិនឃើញ Category លេខ: " + id));
        return mapToDTO(category);
    }

    @Override
    public CategoryDTO updateCategory(Long id, CategoryDTO categoryDTO) {
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("រកមិនឃើញ Category លេខ: " + id));

        String nextSlug = categoryDTO.getSlug() == null || categoryDTO.getSlug().isBlank()
                ? normalizeSlug(categoryDTO.getCatName())
                : normalizeSlug(categoryDTO.getSlug());

        if (categoryRepository.existsByCatNameAndCatIdNot(categoryDTO.getCatName(), id)) {
            throw new DuplicateResourceException("ឈ្មោះប្រភេទ '" + categoryDTO.getCatName() + "' មានរួចហើយ!");
        }

        if (nextSlug != null && categoryRepository.existsBySlugAndCatIdNot(nextSlug, id)) {
            throw new DuplicateResourceException("Slug '" + nextSlug + "' មានរួចហើយ!");
        }

        // Update ព័ត៌មានថ្មី
        existingCategory.setCatName(categoryDTO.getCatName());
        existingCategory.setSlug(nextSlug);
        existingCategory.setCatDesc(categoryDTO.getCatDesc());
        existingCategory.setImageUrl(categoryDTO.getImageUrl());
        existingCategory.setIsActive(categoryDTO.getIsActive() == null ? existingCategory.getIsActive() : categoryDTO.getIsActive());
        existingCategory.setSortOrder(categoryDTO.getSortOrder() == null ? existingCategory.getSortOrder() : categoryDTO.getSortOrder());

        Category updatedCategory = categoryRepository.save(existingCategory);
        return mapToDTO(updatedCategory);
    }

    @Override
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("រកមិនឃើញ Category លេខ: " + id));
        categoryRepository.delete(category);
    }
}