package com.example.demo_project_spring_boot.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public interface DashboardService {
    
    // Overview statistics
    Map<String, Object> getOverviewStats();
    
    // Revenue analytics
    Map<String, Object> getRevenueAnalytics(LocalDateTime startDate, LocalDateTime endDate);
    
    // Order statistics
    Map<String, Object> getOrderStats();
    
    // Product performance
    Map<String, Object> getProductPerformance();
    
    // User analytics
    Map<String, Object> getUserAnalytics();
    
    // Sales by category
    Map<String, Object> getSalesByCategory();
    
    // Recent activity
    Map<String, Object> getRecentActivity(int limit);
    
    // Low stock alerts
    Map<String, Object> getLowStockAlerts(int threshold);
}
