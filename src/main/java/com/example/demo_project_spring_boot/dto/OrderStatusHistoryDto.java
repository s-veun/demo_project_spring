package com.example.demo_project_spring_boot.dto;

import com.example.demo_project_spring_boot.Enum.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderStatusHistoryDto {
    
    private Long id;
    private OrderStatus status;
    private String statusDescription;
    private String note;
    private String updatedBy;
    private LocalDateTime createdAt;
}
