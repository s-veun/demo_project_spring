package com.example.demo_project_spring_boot.service.impl;

import com.example.demo_project_spring_boot.Enum.OrderStatus;
import com.example.demo_project_spring_boot.Enum.Role;
import com.example.demo_project_spring_boot.model.*;
import com.example.demo_project_spring_boot.repository.*;
import com.example.demo_project_spring_boot.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final OrderRepository orderRepository;
    private final ProductReposity productReposity;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final CartRepository cartRepository;

    @Override
    public Map<String, Object> getOverviewStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // Total counts
        stats.put("totalProducts", productReposity.count());
        stats.put("totalUsers", userRepository.count());
        stats.put("totalOrders", orderRepository.count());
        stats.put("totalReviews", reviewRepository.count());
        
        // Revenue calculation
        List<Order> allOrders = orderRepository.findAll();
        BigDecimal totalRevenue = allOrders.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalRevenue", totalRevenue);
        
        // Active users (last 30 days)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long activeUsers = userRepository.findAll().stream()
                .filter(u -> u.getLastLoginAt() != null && u.getLastLoginAt().isAfter(thirtyDaysAgo))
                .count();
        stats.put("activeUsers", activeUsers);
        
        // Pending orders
        long pendingOrders = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PENDING)
                .count();
        stats.put("pendingOrders", pendingOrders);
        
        // Low stock products
        long lowStockProducts = productReposity.findAll().stream()
                .filter(p -> p.getStock() != null && p.getStock() < 10)
                .count();
        stats.put("lowStockProducts", lowStockProducts);
        
        return stats;
    }

    @Override
    public Map<String, Object> getRevenueAnalytics(LocalDateTime startDate, LocalDateTime endDate) {
        List<Order> orders = orderRepository.findAll().stream()
                .filter(o -> o.getOrderDate().isAfter(startDate) && o.getOrderDate().isBefore(endDate))
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .collect(Collectors.toList());

        Map<String, Object> analytics = new HashMap<>();
        analytics.put("totalOrders", orders.size());
        
        BigDecimal revenue = orders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        analytics.put("revenue", revenue);
        
        BigDecimal averageOrderValue = orders.isEmpty() ? BigDecimal.ZERO 
                : revenue.divide(BigDecimal.valueOf(orders.size()), 2, BigDecimal.ROUND_HALF_UP);
        analytics.put("averageOrderValue", averageOrderValue);
        
        // Group by status
        Map<OrderStatus, Long> ordersByStatus = orders.stream()
                .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));
        analytics.put("ordersByStatus", ordersByStatus);
        
        return analytics;
    }

    @Override
    public Map<String, Object> getOrderStats() {
        List<Order> orders = orderRepository.findAll();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOrders", orders.size());
        
        // Orders by status
        Map<OrderStatus, Long> byStatus = orders.stream()
                .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));
        stats.put("byStatus", byStatus);
        
        // Orders today
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        long todayOrders = orders.stream()
                .filter(o -> o.getOrderDate().isAfter(todayStart))
                .count();
        stats.put("todayOrders", todayOrders);
        
        // This month
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();
        long monthOrders = orders.stream()
                .filter(o -> o.getOrderDate().isAfter(monthStart))
                .count();
        stats.put("monthOrders", monthOrders);
        
        return stats;
    }

    @Override
    public Map<String, Object> getProductPerformance() {
        List<Product> products = productReposity.findAll();
        
        Map<String, Object> performance = new HashMap<>();
        
        // Top 10 most viewed
        List<Product> topViewed = products.stream()
                .sorted(Comparator.comparing(Product::getViewCount, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .collect(Collectors.toList());
        performance.put("topViewed", topViewed);
        
        // Top 10 best sellers
        List<Product> topSellers = products.stream()
                .sorted(Comparator.comparing(Product::getPurchaseCount, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .collect(Collectors.toList());
        performance.put("topSellers", topSellers);
        
        // Top 10 highest rated
        List<Product> topRated = products.stream()
                .filter(p -> p.getRating() != null)
                .sorted(Comparator.comparing(Product::getRating).reversed())
                .limit(10)
                .collect(Collectors.toList());
        performance.put("topRated", topRated);
        
        // Out of stock
        List<Product> outOfStock = products.stream()
                .filter(p -> p.getStock() == null || p.getStock() == 0)
                .collect(Collectors.toList());
        performance.put("outOfStock", outOfStock);
        
        return performance;
    }

    @Override
    public Map<String, Object> getUserAnalytics() {
        List<User> users = userRepository.findAll();
        
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("totalUsers", users.size());
        
        // By role
        Map<Role, Long> byRole = users.stream()
                .collect(Collectors.groupingBy(User::getRole, Collectors.counting()));
        analytics.put("byRole", byRole);
        
        // Active users (last 7 days)
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        long activeUsers = users.stream()
                .filter(u -> u.getLastLoginAt() != null && u.getLastLoginAt().isAfter(sevenDaysAgo))
                .count();
        analytics.put("activeUsers", activeUsers);
        
        // New users this month
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();
        long newUsers = users.stream()
                .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(monthStart))
                .count();
        analytics.put("newUsersThisMonth", newUsers);
        
        return analytics;
    }

    @Override
    public Map<String, Object> getSalesByCategory() {
        // This would require joining orders with products and categories
        // Simplified version - can be enhanced with proper JPA queries
        Map<String, Object> sales = new HashMap<>();
        sales.put("message", "Category sales analytics - implement with custom JPQL query");
        return sales;
    }

    @Override
    public Map<String, Object> getRecentActivity(int limit) {
        Map<String, Object> activity = new HashMap<>();
        
        // Recent orders
        List<Order> recentOrders = orderRepository.findAll().stream()
                .sorted(Comparator.comparing(Order::getOrderDate).reversed())
                .limit(limit)
                .collect(Collectors.toList());
        activity.put("recentOrders", recentOrders);
        
        // Recent reviews
        List<Review> recentReviews = reviewRepository.findAll().stream()
                .sorted(Comparator.comparing(Review::getCreatedAt).reversed())
                .limit(limit)
                .collect(Collectors.toList());
        activity.put("recentReviews", recentReviews);
        
        return activity;
    }

    @Override
    public Map<String, Object> getLowStockAlerts(int threshold) {
        List<Product> lowStock = productReposity.findAll().stream()
                .filter(p -> p.getStock() != null && p.getStock() < threshold)
                .sorted(Comparator.comparing(Product::getStock))
                .collect(Collectors.toList());
        
        Map<String, Object> alerts = new HashMap<>();
        alerts.put("count", lowStock.size());
        alerts.put("products", lowStock);
        alerts.put("threshold", threshold);
        
        return alerts;
    }
}
