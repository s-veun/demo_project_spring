package com.example.demo_project_spring_boot.service.impl;

import com.example.demo_project_spring_boot.dto.CategoryDTO;
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
                category.getCatDesc()
        );
    }

    // 🛠 មុខងារជំនួយទី២៖ បំប្លែងពី DTO ទៅជា Entity (សម្រាប់ Save ចូល Database)
    private Category mapToEntity(CategoryDTO dto) {
        Category category = new Category();
        category.setCatName(dto.getCatName());
        category.setCatDesc(dto.getCatDesc());
        return category;
    }

    @Override
    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        // ឆែកមើលក្រែងលោមានឈ្មោះជាន់គ្នា
        if (categoryRepository.existsByCatName(categoryDTO.getCatName())) {
            throw new RuntimeException("ឈ្មោះប្រភេទ '" + categoryDTO.getCatName() + "' មានរួចហើយ!");
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
                .orElseThrow(() -> new RuntimeException("រកមិនឃើញ Category លេខ: " + id));
        return mapToDTO(category);
    }

    @Override
    public CategoryDTO updateCategory(Long id, CategoryDTO categoryDTO) {
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("រកមិនឃើញ Category លេខ: " + id));

        // Update ព័ត៌មានថ្មី
        existingCategory.setCatName(categoryDTO.getCatName());
        existingCategory.setCatDesc(categoryDTO.getCatDesc());

        Category updatedCategory = categoryRepository.save(existingCategory);
        return mapToDTO(updatedCategory);
    }

    @Override
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("រកមិនឃើញ Category លេខ: " + id));
        categoryRepository.delete(category);
    }
}