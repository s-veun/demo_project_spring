package com.example.demo_project_spring_boot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductListDTO {
    private Long proId;
    private String proName;
    private String sku;
    private BigDecimal proPrice;
    private String proBrand;
    private String categoryName;
    private Long categoryId;
    private String imageUrl;
    private Double discount;
    private Integer stock;
    private Boolean available;
    private String tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
