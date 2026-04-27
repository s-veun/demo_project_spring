package com.example.demo_project_spring_boot.controller;

import com.example.demo_project_spring_boot.Enum.OrderStatus;
import com.example.demo_project_spring_boot.dto.OrderStatusHistoryDto;
import com.example.demo_project_spring_boot.dto.OrderStatusUpdateRequestDto;
import com.example.demo_project_spring_boot.dto.OrderTrackingResponseDto;
import com.example.demo_project_spring_boot.model.Order;
import com.example.demo_project_spring_boot.service.OrderTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Order Tracking", description = "Order tracking and status management APIs")
public class OrderTrackingController {

    private final OrderTrackingService orderTrackingService;

    // 1. Track Order - Get full tracking details
    @GetMapping("/{orderId}/tracking")
    @Operation(summary = "Track order with full status history and timeline")
    public ResponseEntity<?> trackOrder(@PathVariable Long orderId) {
        try {
            OrderTrackingResponseDto tracking = orderTrackingService.trackOrder(orderId);
            return ResponseEntity.ok(tracking);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // 2. Get Order Status History
    @GetMapping("/{orderId}/status-history")
    @Operation(summary = "Get order status change history")
    public ResponseEntity<?> getOrderStatusHistory(@PathVariable Long orderId) {
        try {
            List<OrderStatusHistoryDto> history = orderTrackingService.getOrderStatusHistory(orderId);
            return ResponseEntity.ok(Map.of(
                    "orderId", orderId,
                    "historyCount", history.size(),
                    "history", history
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // 3. Get Latest Status
    @GetMapping("/{orderId}/latest-status")
    @Operation(summary = "Get latest order status")
    public ResponseEntity<?> getLatestStatus(@PathVariable Long orderId) {
        try {
            OrderStatusHistoryDto latestStatus = orderTrackingService.getLatestStatus(orderId);
            return ResponseEntity.ok(latestStatus);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // 4. Update Order Status (Admin only)
    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update order status (Admin only)")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody OrderStatusUpdateRequestDto request) {
        try {
            orderTrackingService.updateOrderStatus(orderId, request);
            return ResponseEntity.ok(Map.of(
                    "message", "Order status updated successfully",
                    "orderId", orderId,
                    "newStatus", request.getStatus(),
                    "statusDescription", request.getStatus().getDescription()
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update order status: " + e.getMessage()));
        }
    }

    // 5. Get Orders by Status (Admin only)
    @GetMapping("/by-status")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get all orders by status (Admin only)")
    public ResponseEntity<?> getOrdersByStatus(@RequestParam OrderStatus status) {
        try {
            List<Order> orders = orderTrackingService.getOrdersByStatus(status);
            return ResponseEntity.ok(Map.of(
                    "status", status,
                    "statusDescription", status.getDescription(),
                    "count", orders.size(),
                    "orders", orders
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // 6. Get Order Count by Status (Admin only)
    @GetMapping("/count-by-status")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get order count by status (Admin only)")
    public ResponseEntity<?> getOrderCountByStatus(@RequestParam OrderStatus status) {
        try {
            Long count = orderTrackingService.getOrderCountByStatus(status);
            return ResponseEntity.ok(Map.of(
                    "status", status,
                    "statusDescription", status.getDescription(),
                    "count", count
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // 7. Get All Available Statuses
    @GetMapping("/statuses")
    @Operation(summary = "Get all available order statuses")
    public ResponseEntity<?> getAllStatuses() {
        OrderStatus[] statuses = OrderStatus.values();
        return ResponseEntity.ok(Map.of(
                "totalStatuses", statuses.length,
                "statuses", statuses
        ));
    }
}
