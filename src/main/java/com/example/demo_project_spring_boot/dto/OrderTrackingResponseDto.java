package com.example.demo_project_spring_boot.dto;

import com.example.demo_project_spring_boot.Enum.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderTrackingResponseDto {
    
    private Long orderId;
    private String orderNumber;
    private OrderStatus currentStatus;
    private String currentStatusDescription;
    private LocalDateTime orderDate;
    private LocalDateTime estimatedDelivery;
    private List<OrderStatusHistoryDto> statusHistory;
    private TrackingTimelineDto timeline;
}
