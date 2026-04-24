package com.example.demo_project_spring_boot.service;

import com.example.demo_project_spring_boot.dto.OrderRequestDto;
import com.example.demo_project_spring_boot.model.Order;
import com.example.demo_project_spring_boot.Enum.OrderStatus;

public interface OrderService {
    Order placeOrder(OrderRequestDto requestDto);
    Order updateOrderStatus(Long orderId, OrderStatus orderStatus);
    Order cancelOrder(Long orderId);
}
