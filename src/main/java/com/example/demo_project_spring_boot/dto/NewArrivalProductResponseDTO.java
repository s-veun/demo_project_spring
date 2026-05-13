package com.example.demo_project_spring_boot.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * DTO for new arrival product response
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NewArrivalProductResponseDTO {

    private Long proId;
    private String proName;
    private String sku;
    private String proDesc;
    private BigDecimal proPrice;
    private String proBrand;
    private Double discount;
    private Integer stock;
    private String tags;
    private Boolean available;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date releaseDate;

    private String categoryName;
    private Long categoryId;

    private String imageUrl;  // Main image
    private List<String> imageUrls;  // All images

    private String arrivalNotes;
    private Integer daysToShowAsNew;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    private Boolean isNew;  // Calculated: true if createdAt is within daysToShowAsNew

    private Double weight;
    private Double length;
    private Double width;
    private Double height;
}

