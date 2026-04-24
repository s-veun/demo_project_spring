package com.example.demo_project_spring_boot.controller;

import com.example.demo_project_spring_boot.dto.OrderItemResponseDto;
import com.example.demo_project_spring_boot.dto.OrderRequestDto; // 🌟 កុំភ្លេច Import DTO នេះ
import com.example.demo_project_spring_boot.dto.OrderResponseDto;
import com.example.demo_project_spring_boot.model.Order;
import com.example.demo_project_spring_boot.Enum.OrderStatus;
import com.example.demo_project_spring_boot.repository.OrderRepository;
import com.example.demo_project_spring_boot.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;

    // ១. API សម្រាប់បញ្ជាទិញ (Checkout)
    // 🌟 កែពី /{userId}/checkout មកជា /checkout ទទេវិញ ហើយទទួលយក @RequestBody
    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestBody OrderRequestDto requestDto) {
        try {
            // 🌟 បោះ requestDto ចូលទៅក្នុង Service
            Order order = orderService.placeOrder(requestDto);

            return ResponseEntity.ok(mapToOrderResponseDto(order));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ២. API សម្រាប់មើលប្រវត្តិទិញទំនិញទាំងអស់របស់ User ម្នាក់ (Order History)
    @GetMapping("/{userId}/history")
    public ResponseEntity<List<OrderResponseDto>> getOrderHistory(@PathVariable Long userId) {

        List<Order> orders = orderRepository.findByUser_IdOrderByOrderDateDesc(userId);

        List<OrderResponseDto> orderDtos = orders.stream()
                .map(this::mapToOrderResponseDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(orderDtos);
    }

    @PutMapping("/{orderId}/status")
    @Transactional
    public ResponseEntity<OrderResponseDto> updateStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status) {

        Order updatedOrder = orderService.updateOrderStatus(orderId, status);

        return ResponseEntity.ok(mapToOrderResponseDto(updatedOrder));
    }

    @PutMapping("/{orderId}/cancel")
    @Transactional
    public ResponseEntity<OrderResponseDto> cancelOrder(@PathVariable Long orderId) {

        Order cancelledOrder = orderService.cancelOrder(orderId);

        return ResponseEntity.ok(mapToOrderResponseDto(cancelledOrder));
    }

    // =====================================================================
    // Helper Method សម្រាប់បំប្លែង Order (Entity) ទៅជា OrderResponseDto (DTO)
    // =====================================================================
    private OrderResponseDto mapToOrderResponseDto(Order order) {

        List<OrderItemResponseDto> itemDtos = order.getOrderItems().stream().map(item ->
                OrderItemResponseDto.builder()
                        .id(item.getId())
                        .productId(item.getProduct().getProId())
                        .productName(item.getProduct().getProName())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .subTotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                        .build()
        ).collect(Collectors.toList());

        return OrderResponseDto.builder()
                .orderId(order.getOrderId())
                .orderDate(order.getOrderDate())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .items(itemDtos)
                .build();
    }
}