package com.example.demo_project_spring_boot.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class ProductResponseDTO {

    private Long proId;

    private String proName;

    private String proDesc;

    private BigDecimal proPrice;

    private String proBrand;

    // category info
    private String categoryName;

    private Integer stock;

    private Boolean available;

    private Date releaseDate;

    // thumbnail image for product card
    private String thumbnailImage;

    // all product images
    private List<ProductImageResponseDTO> images;
}