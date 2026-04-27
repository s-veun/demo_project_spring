package com.example.demo_project_spring_boot.dto;

import com.example.demo_project_spring_boot.Enum.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderStatusUpdateRequestDto {
    
    @NotNull(message = "Status is required")
    private OrderStatus status;
    
    private String note;
    
    private String updatedBy;
}
