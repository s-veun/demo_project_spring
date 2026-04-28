package com.example.demo_project_spring_boot.dto;

import com.example.demo_project_spring_boot.Enum.OrderStatus;
import com.example.demo_project_spring_boot.Enum.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderSearchRequest {
    private String keyword; // orderId, customer name, email
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Long userId;
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortOrder;
}
