package com.example.demo_project_spring_boot.service.impl;

import com.example.demo_project_spring_boot.Enum.OrderStatus;
import com.example.demo_project_spring_boot.Enum.Role;
import com.example.demo_project_spring_boot.model.*;
import com.example.demo_project_spring_boot.repository.*;
import com.example.demo_project_spring_boot.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
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
                : revenue.divide(BigDecimal.valueOf(orders.size()), 2, RoundingMode.HALF_UP);
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

    @Override
    public Map<String, Object> getPerformanceMetrics(String period) {
        Map<String, Object> current = getMetricsForPeriod(period, 0);
        Map<String, Object> previous = getMetricsForPeriod(period, 1);

        double revenueGrowth = growthPercent((BigDecimal) current.get("totalRevenue"), (BigDecimal) previous.get("totalRevenue"));
        double orderGrowth = growthPercent((Number) current.get("totalOrders"), (Number) previous.get("totalOrders"));
        double userGrowth = growthPercent((Number) current.get("newUsers"), (Number) previous.get("newUsers"));
        double productGrowth = growthPercent((Number) current.get("newProducts"), (Number) previous.get("newProducts"));

        return Map.of(
                "period", period == null || period.isBlank() ? "monthly" : period,
                "revenueGrowth", revenueGrowth,
                "orderGrowth", orderGrowth,
                "userGrowth", userGrowth,
                "productGrowth", productGrowth,
                "current", current,
                "previous", previous
        );
    }

    @Override
    public Map<String, Object> getUserEngagement() {
        List<User> users = userRepository.findAll();
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        long activeUsers = users.stream()
                .filter(u -> u.getLastLoginAt() != null && u.getLastLoginAt().isAfter(sevenDaysAgo))
                .count();
        long inactiveUsers = users.size() - activeUsers;

        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();
        long newUsersThisMonth = users.stream()
                .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(monthStart))
                .count();

        long returningUsers = users.stream()
                .filter(u -> u.getLastLoginAt() != null && u.getCreatedAt() != null)
                .filter(u -> u.getCreatedAt().isBefore(sevenDaysAgo) && u.getLastLoginAt().isAfter(sevenDaysAgo))
                .count();

        return Map.of(
                "totalUsers", users.size(),
                "activeUsers", activeUsers,
                "inactiveUsers", inactiveUsers,
                "newUsersThisMonth", newUsersThisMonth,
                "returningUsers", returningUsers
        );
    }

    @Override
    public Map<String, Object> getInventorySummary(int threshold) {
        List<Product> products = productReposity.findAll();

        long outOfStockItems = products.stream().filter(p -> p.getStock() == null || p.getStock() == 0).count();
        long lowStockItems = products.stream().filter(p -> p.getStock() != null && p.getStock() > 0 && p.getStock() <= threshold).count();
        long overStockedItems = products.stream().filter(p -> p.getStock() != null && p.getStock() > threshold * 5L).count();
        long adequateStockItems = products.size() - outOfStockItems - lowStockItems;

        return Map.of(
                "totalItems", products.size(),
                "outOfStockItems", outOfStockItems,
                "lowStockItems", lowStockItems,
                "adequateStockItems", Math.max(adequateStockItems, 0),
                "overStockedItems", overStockedItems
        );
    }

    @Override
    public Map<String, Object> getPaymentStatus() {
        List<Order> orders = orderRepository.findAll();

        long completed = orders.stream().filter(o -> o.getPaymentStatus() == com.example.demo_project_spring_boot.Enum.PaymentStatus.COMPLETED).count();
        long pending = orders.stream().filter(o -> o.getPaymentStatus() == com.example.demo_project_spring_boot.Enum.PaymentStatus.PENDING).count();
        long failed = orders.stream().filter(o -> o.getPaymentStatus() == com.example.demo_project_spring_boot.Enum.PaymentStatus.FAILED).count();
        long refunded = orders.stream().filter(o -> o.getPaymentStatus() == com.example.demo_project_spring_boot.Enum.PaymentStatus.REFUNDED).count();

        return Map.of(
                "completed", completed,
                "pending", pending,
                "failed", failed,
                "refunded", refunded
        );
    }

    @Override
    public Map<String, Object> getMetricsComparison(String period) {
        String normalizedPeriod = period == null || period.isBlank() ? "month" : period;
        Map<String, Object> current = getMetricsForPeriod(normalizedPeriod, 0);
        Map<String, Object> previous = getMetricsForPeriod(normalizedPeriod, 1);

        Map<String, Number> growth = Map.of(
                "totalRevenue", growthPercent((BigDecimal) current.get("totalRevenue"), (BigDecimal) previous.get("totalRevenue")),
                "totalOrders", growthPercent((Number) current.get("totalOrders"), (Number) previous.get("totalOrders")),
                "newUsers", growthPercent((Number) current.get("newUsers"), (Number) previous.get("newUsers")),
                "newProducts", growthPercent((Number) current.get("newProducts"), (Number) previous.get("newProducts"))
        );

        return Map.of(
                "current", current,
                "previous", previous,
                "growth", growth
        );
    }

    @Override
    public Map<String, Object> getDashboardAnalytics() {
        List<Order> orders = orderRepository.findAll();
        List<Product> products = productReposity.findAll();
        List<User> users = userRepository.findAll();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekStart = now.with(DayOfWeek.MONDAY).toLocalDate().atStartOfDay();
        LocalDateTime monthStart = now.withDayOfMonth(1).toLocalDate().atStartOfDay();

        BigDecimal weeklyRevenue = orders.stream()
                .filter(o -> o.getOrderDate() != null && o.getOrderDate().isAfter(weekStart))
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal monthlyRevenue = orders.stream()
                .filter(o -> o.getOrderDate() != null && o.getOrderDate().isAfter(monthStart))
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Map<String, Object>> topSellingProducts = products.stream()
                .sorted(Comparator.comparing(Product::getPurchaseCount, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .map(p -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("productId", p.getProId());
                    row.put("name", p.getProName());
                    row.put("purchaseCount", p.getPurchaseCount() == null ? 0 : p.getPurchaseCount());
                    row.put("stock", p.getStock() == null ? 0 : p.getStock());
                    return row;
                })
                .toList();

        List<Map<String, Object>> recentOrders = orders.stream()
                .sorted(Comparator.comparing(Order::getOrderDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .map(order -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("orderId", order.getOrderId());
                    row.put("orderDate", order.getOrderDate());
                    row.put("status", order.getStatus());
                    row.put("totalAmount", order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount());
                    return row;
                })
                .toList();

        long activeUsersCount = users.stream()
                .filter(u -> u.getLastLoginAt() != null && u.getLastLoginAt().isAfter(now.minusDays(30)))
                .count();

        return Map.of(
                "totalUsers", users.size(),
                "totalProducts", products.size(),
                "totalOrders", orders.size(),
                "totalRevenue", orders.stream()
                        .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                        .map(Order::getTotalAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                "weekly", Map.of(
                        "orders", orders.stream().filter(o -> o.getOrderDate() != null && o.getOrderDate().isAfter(weekStart)).count(),
                        "revenue", weeklyRevenue
                ),
                "monthly", Map.of(
                        "orders", orders.stream().filter(o -> o.getOrderDate() != null && o.getOrderDate().isAfter(monthStart)).count(),
                        "revenue", monthlyRevenue
                ),
                "topSellingProducts", topSellingProducts,
                "recentOrders", recentOrders,
                "activeUsersCount", activeUsersCount
        );
    }

    private Map<String, Object> getMetricsForPeriod(String period, int periodOffset) {
        TimeWindow timeWindow = resolveWindow(period, periodOffset);

        List<Order> periodOrders = orderRepository.findAll().stream()
                .filter(o -> o.getOrderDate() != null
                        && !o.getOrderDate().isBefore(timeWindow.start())
                        && o.getOrderDate().isBefore(timeWindow.end()))
                .toList();

        BigDecimal totalRevenue = periodOrders.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long newUsers = userRepository.findAll().stream()
                .filter(u -> u.getCreatedAt() != null
                        && !u.getCreatedAt().isBefore(timeWindow.start())
                        && u.getCreatedAt().isBefore(timeWindow.end()))
                .count();

        long newProducts = productReposity.findAll().stream()
                .filter(p -> p.getCreatedAt() != null
                        && !p.getCreatedAt().isBefore(timeWindow.start())
                        && p.getCreatedAt().isBefore(timeWindow.end()))
                .count();

        return Map.of(
                "start", timeWindow.start(),
                "end", timeWindow.end(),
                "totalOrders", periodOrders.size(),
                "totalRevenue", totalRevenue,
                "newUsers", newUsers,
                "newProducts", newProducts
        );
    }

    private TimeWindow resolveWindow(String period, int offset) {
        LocalDateTime now = LocalDateTime.now();
        String normalized = period == null || period.isBlank() ? "monthly" : period.toLowerCase(Locale.ROOT);

        return switch (normalized) {
            case "daily", "day" -> {
                LocalDateTime start = now.toLocalDate().atStartOfDay().minusDays(offset);
                yield new TimeWindow(start, start.plusDays(1));
            }
            case "weekly", "week" -> {
                LocalDateTime start = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                        .toLocalDate().atStartOfDay().minusWeeks(offset);
                yield new TimeWindow(start, start.plusWeeks(1));
            }
            case "yearly", "year" -> {
                LocalDateTime start = now.withDayOfYear(1).toLocalDate().atStartOfDay().minusYears(offset);
                yield new TimeWindow(start, start.plusYears(1));
            }
            default -> {
                LocalDateTime start = now.withDayOfMonth(1).toLocalDate().atStartOfDay().minusMonths(offset);
                yield new TimeWindow(start, start.plusMonths(1));
            }
        };
    }

    private double growthPercent(Number current, Number previous) {
        double currentValue = current == null ? 0d : current.doubleValue();
        double previousValue = previous == null ? 0d : previous.doubleValue();
        if (previousValue == 0d) {
            return currentValue == 0d ? 0d : 100d;
        }
        return ((currentValue - previousValue) / previousValue) * 100d;
    }

    private double growthPercent(BigDecimal current, BigDecimal previous) {
        BigDecimal safeCurrent = current == null ? BigDecimal.ZERO : current;
        BigDecimal safePrevious = previous == null ? BigDecimal.ZERO : previous;
        if (safePrevious.compareTo(BigDecimal.ZERO) == 0) {
            return safeCurrent.compareTo(BigDecimal.ZERO) == 0 ? 0d : 100d;
        }
        return safeCurrent.subtract(safePrevious)
                .multiply(BigDecimal.valueOf(100))
                .divide(safePrevious, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private record TimeWindow(LocalDateTime start, LocalDateTime end) {
    }
}
