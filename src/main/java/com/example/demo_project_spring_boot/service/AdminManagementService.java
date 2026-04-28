package com.example.demo_project_spring_boot.service;

import com.example.demo_project_spring_boot.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public interface AdminManagementService {
    
    // User Management
    Page<UserProfileResponse> searchUsers(UserSearchRequest request);
    Map<String, Object> bulkUserAction(BulkUserActionRequest request);
    Map<String, Object> getUserDetails(Long userId);
    
    // Order Management  
    Page<OrderResponseDto> searchOrders(OrderSearchRequest request);
    Map<String, Object> bulkOrderStatusUpdate(List<Long> orderIds, String newStatus, String note);
    Map<String, Object> getOrderDetails(Long orderId);
    
    // Product Management
    Map<String, Object> updateProductStock(Long productId, Integer newStock, String reason);
    Map<String, Object> getLowStockProducts(Integer threshold);
    Map<String, Object> getOutOfStockProducts();
    
    // Reporting
    Map<String, Object> generateReport(ReportRequest request);
    Map<String, Object> getSalesReport(LocalDateTime startDate, LocalDateTime endDate);
    Map<String, Object> getUserGrowthReport(LocalDateTime startDate, LocalDateTime endDate);
    
    // Analytics
    Map<String, Object> getAdvancedAnalytics();
    Map<String, Object> getTopSellingProducts(Integer limit);
    Map<String, Object> getTopCustomers(Integer limit);
}
