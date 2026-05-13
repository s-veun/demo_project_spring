package com.example.demo_project_spring_boot.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryDTO {
    private Long catId;

    @NotBlank(message = "Category name is required")
    private String catName;

    @NotBlank(message = "Slug is required")
    private String slug;
    private String catDesc;
    private String imageUrl;
    private Boolean isActive;
    private Integer sortOrder;

}