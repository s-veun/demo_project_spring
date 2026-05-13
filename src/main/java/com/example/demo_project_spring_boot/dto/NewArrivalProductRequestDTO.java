package com.example.demo_project_spring_boot.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * DTO for creating a new arrival product
 * Extends ProductRequestDTO with additional fields for new arrivals
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NewArrivalProductRequestDTO extends ProductRequestDTO {

    // New arrival specific fields
    private String releaseDate;      // ISO 8601 format: "2024-05-13"
    private Boolean isNewArrival;    // Mark as new arrival
    private String arrivalNotes;     // Additional notes for new arrivals
    private Integer daysToShowAsNew; // Days to show as "New" (default: 30)
    private List<String> imageUrls;  // Pre-uploaded image URLs
}

