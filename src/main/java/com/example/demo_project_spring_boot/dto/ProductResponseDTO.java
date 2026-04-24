package com.example.demo_project_spring_boot.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class ProductResponseDTO {
    private Long proId;
    private String proName;
    private String proDesc;
    private BigDecimal proPrice;
    private String proBrand;

    // 🌟 ចំណុចសំខាន់៖ ពេលបង្ហាញទំនិញ យើងបង្ហាញឈ្មោះ Category តែម្តង ងាយស្រួលដល់ Frontend យកទៅប្រើ
    private String categoryName;

    private Integer stock;
    private Boolean available;
    private Date releaseDate;
    private String imageName;
}
