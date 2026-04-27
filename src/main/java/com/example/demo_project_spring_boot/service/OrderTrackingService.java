package com.example.demo_project_spring_boot.service;

import com.example.demo_project_spring_boot.Enum.OrderStatus;
import com.example.demo_project_spring_boot.dto.OrderStatusHistoryDto;
import com.example.demo_project_spring_boot.dto.OrderStatusUpdateRequestDto;
import com.example.demo_project_spring_boot.dto.OrderTrackingResponseDto;
import com.example.demo_project_spring_boot.dto.TrackingTimelineDto;
import com.example.demo_project_spring_boot.exception.ResourceNotFoundException;
import com.example.demo_project_spring_boot.model.Order;
import com.example.demo_project_spring_boot.model.OrderStatusHistory;
import com.example.demo_project_spring_boot.repository.OrderRepository;
import com.example.demo_project_spring_boot.repository.OrderStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderTrackingService {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;

    @Transactional
    public OrderTrackingResponseDto trackOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        List<OrderStatusHistory> history = statusHistoryRepository.findByOrderOrderIdOrderByCreatedAtDesc(orderId);
        
        OrderTrackingResponseDto response = OrderTrackingResponseDto.builder()
                .orderId(order.getOrderId())
                .orderNumber(order.getOrderId().toString())
                .currentStatus(order.getStatus())
                .currentStatusDescription(order.getStatus().getDescription())
                .orderDate(order.getOrderDate())
                .estimatedDelivery(order.getDeliveredDate())
                .statusHistory(history.stream()
                        .map(this::mapToDto)
                        .collect(Collectors.toList()))
                .timeline(buildTimeline(history))
                .build();

        return response;
    }

    @Transactional
    public OrderStatusHistory updateOrderStatus(Long orderId, OrderStatusUpdateRequestDto request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        OrderStatus currentStatus = order.getStatus();
        OrderStatus newStatus = request.getStatus();

        // Validate status transition
        if (!currentStatus.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    String.format("Cannot transition from %s to %s", currentStatus, newStatus));
        }

        // Update order status
        order.setStatus(newStatus);
        orderRepository.save(order);

        // Create status history entry
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(newStatus)
                .note(request.getNote())
                .updatedBy(request.getUpdatedBy())
                .build();

        return statusHistoryRepository.save(history);
    }

    @Transactional
    public List<OrderStatusHistoryDto> getOrderStatusHistory(Long orderId) {
        if (!orderRepository.existsById(orderId)) {
            throw new ResourceNotFoundException("Order not found with id: " + orderId);
        }

        List<OrderStatusHistory> history = statusHistoryRepository.findByOrderOrderIdOrderByCreatedAtDesc(orderId);
        return history.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderStatusHistoryDto getLatestStatus(Long orderId) {
        if (!orderRepository.existsById(orderId)) {
            throw new ResourceNotFoundException("Order not found with id: " + orderId);
        }

        OrderStatusHistory latestStatus = statusHistoryRepository
                .findTopByOrderOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("No status history found for order: " + orderId));

        return mapToDto(latestStatus);
    }

    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    public Long getOrderCountByStatus(OrderStatus status) {
        return statusHistoryRepository.countByStatus(status);
    }

    private OrderStatusHistoryDto mapToDto(OrderStatusHistory history) {
        return OrderStatusHistoryDto.builder()
                .id(history.getId())
                .status(history.getStatus())
                .statusDescription(history.getStatus().getDescription())
                .note(history.getNote())
                .updatedBy(history.getUpdatedBy())
                .createdAt(history.getCreatedAt())
                .build();
    }

    private TrackingTimelineDto buildTimeline(List<OrderStatusHistory> history) {
        TrackingTimelineDto timeline = TrackingTimelineDto.builder()
                .progressPercentage(0)
                .build();

        for (OrderStatusHistory record : history) {
            switch (record.getStatus()) {
                case PENDING:
                    if (timeline.getOrderPlaced() == null) {
                        timeline.setOrderPlaced(record.getCreatedAt());
                    }
                    break;
                case CONFIRMED:
                    if (timeline.getConfirmed() == null) {
                        timeline.setConfirmed(record.getCreatedAt());
                    }
                    break;
                case PROCESSING:
                    if (timeline.getProcessing() == null) {
                        timeline.setProcessing(record.getCreatedAt());
                    }
                    break;
                case PAID:
                    if (timeline.getPaid() == null) {
                        timeline.setPaid(record.getCreatedAt());
                    }
                    break;
                case SHIPPED:
                    if (timeline.getShipped() == null) {
                        timeline.setShipped(record.getCreatedAt());
                    }
                    break;
                case OUT_FOR_DELIVERY:
                    if (timeline.getOutForDelivery() == null) {
                        timeline.setOutForDelivery(record.getCreatedAt());
                    }
                    break;
                case DELIVERED:
                    if (timeline.getDelivered() == null) {
                        timeline.setDelivered(record.getCreatedAt());
                    }
                    break;
                case COMPLETED:
                    if (timeline.getCompleted() == null) {
                        timeline.setCompleted(record.getCreatedAt());
                    }
                    break;
                default:
                    break;
            }
        }

        // Calculate progress percentage
        timeline.setProgressPercentage(calculateProgress(timeline));

        return timeline;
    }

    private int calculateProgress(TrackingTimelineDto timeline) {
        int progress = 0;
        
        if (timeline.getOrderPlaced() != null) progress += 10;
        if (timeline.getConfirmed() != null) progress += 10;
        if (timeline.getProcessing() != null) progress += 10;
        if (timeline.getPaid() != null) progress += 10;
        if (timeline.getShipped() != null) progress += 20;
        if (timeline.getOutForDelivery() != null) progress += 15;
        if (timeline.getDelivered() != null) progress += 15;
        if (timeline.getCompleted() != null) progress += 10;

        return Math.min(progress, 100);
    }
}
