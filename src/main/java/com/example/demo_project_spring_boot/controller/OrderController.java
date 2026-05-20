package com.example.demo_project_spring_boot.controller;

import com.example.demo_project_spring_boot.dto.OrderItemResponseDto;
import com.example.demo_project_spring_boot.dto.OrderRequestDto;
import com.example.demo_project_spring_boot.dto.OrderResponseDto;
import com.example.demo_project_spring_boot.exception.ForbiddenException;
import com.example.demo_project_spring_boot.exception.UnauthorizedException;
import com.example.demo_project_spring_boot.model.Order;
import com.example.demo_project_spring_boot.model.User;
import com.example.demo_project_spring_boot.repository.OrderRepository;
import com.example.demo_project_spring_boot.repository.UserRepository;
import com.example.demo_project_spring_boot.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Orders", description = "Order management APIs")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    /**
     * POST /api/v1/orders/checkout
     * Place a new order from the authenticated user's cart.
     * userId is always taken from JWT — the userId field in the body is ignored for security.
     */
    @PostMapping("/checkout")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    @Operation(summary = "Place a new order (Checkout)",
            description = "Creates an order from the authenticated user's cart. The userId is derived from the JWT token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order placed successfully"),
            @ApiResponse(responseCode = "400", description = "Cart is empty or insufficient stock"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<?> checkout(
            @RequestBody OrderRequestDto requestDto,
            Authentication authentication) {
        try {
            Long userId = resolveUserId(authentication);
            // Always override with authenticated user's ID — never trust userId from request body
            requestDto.setUserId(userId);
            log.info("[Order] Checkout requested by userId={}", userId);
            Order order = orderService.placeOrder(requestDto);
            return ResponseEntity.ok(mapToOrderResponseDto(order));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            log.warn("[Order] Checkout failed for user: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/v1/orders/{userId}/history
     * Users can only see their own order history. Admins can see any user's history.
     */
    @GetMapping("/{userId}/history")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get order history for a user",
            description = "Regular users can only access their own order history. Admins can access any user's history.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order history retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — users can only access their own orders")
    })
    public ResponseEntity<?> getOrderHistory(
            @PathVariable
            @Parameter(description = "User ID", required = true)
            Long userId,
            Authentication authentication) {
        try {
            validateOrderAccess(userId, authentication);
            List<Order> orders = orderRepository.findByUser_IdOrderByOrderDateDesc(userId);
            List<OrderResponseDto> orderDtos = orders.stream()
                    .map(this::mapToOrderResponseDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(orderDtos);
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        } catch (ForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PUT /api/v1/orders/{orderId}/cancel
     * Cancel a pending order. Only the order owner or an admin can cancel.
     */
    @PutMapping("/{orderId}/cancel")
    @Transactional
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancel an order",
            description = "Cancel a PENDING order. Only the order owner or an admin can cancel.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot cancel this order"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<?> cancelOrder(
            @PathVariable
            @Parameter(description = "Order ID to cancel", required = true)
            Long orderId,
            Authentication authentication) {
        try {
            // Validate ownership before cancelling
            Order existing = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
            validateOrderAccess(existing.getUser().getId(), authentication);
            Order cancelledOrder = orderService.cancelOrder(orderId);
            return ResponseEntity.ok(mapToOrderResponseDto(cancelledOrder));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        } catch (ForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Security helpers
    // ─────────────────────────────────────────────────────────────

    private Long resolveUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            throw new UnauthorizedException("Authentication required");
        }
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new UnauthorizedException("Authenticated user not found in database"));
        return user.getId();
    }

    private void validateOrderAccess(Long resourceOwnerId, Authentication authentication) {
        Long requesterId = resolveUserId(authentication);
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !requesterId.equals(resourceOwnerId)) {
            throw new ForbiddenException("You are not allowed to access another user's orders");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Mapping
    // ─────────────────────────────────────────────────────────────

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