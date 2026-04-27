package com.example.demo_project_spring_boot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TrackingTimelineDto {
    
    private LocalDateTime orderPlaced;
    private LocalDateTime confirmed;
    private LocalDateTime processing;
    private LocalDateTime paid;
    private LocalDateTime shipped;
    private LocalDateTime outForDelivery;
    private LocalDateTime delivered;
    private LocalDateTime completed;
    private Integer progressPercentage;
}
