package com.example.demo_project_spring_boot.service;

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

    Map<String, Object> getPerformanceMetrics(String period);

    Map<String, Object> getUserEngagement();

    Map<String, Object> getInventorySummary(int threshold);

    Map<String, Object> getPaymentStatus();

    Map<String, Object> getMetricsComparison(String period);

    Map<String, Object> getDashboardAnalytics();
}
