package com.example.demo_project_spring_boot.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductRequestDTO {
    private String proName;
    private String proDesc;
    private BigDecimal proPrice;
    private String proBrand;

    private Long categoryId;

    private Integer quantity;
    private Double discount;
    private String tags;
}