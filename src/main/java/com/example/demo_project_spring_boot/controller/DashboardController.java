package com.example.demo_project_spring_boot.controller;

import com.example.demo_project_spring_boot.service.DashboardService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin Dashboard", description = "Admin dashboard analytics and statistics APIs")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    public ResponseEntity<?> getOverview() {
        return ResponseEntity.ok(dashboardService.getOverviewStats());
    }

    @GetMapping("/revenue")
    public ResponseEntity<?> getRevenueAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(dashboardService.getRevenueAnalytics(startDate, endDate));
    }

    @GetMapping("/orders")
    public ResponseEntity<?> getOrderStats() {
        return ResponseEntity.ok(dashboardService.getOrderStats());
    }

    @GetMapping("/products")
    public ResponseEntity<?> getProductPerformance() {
        return ResponseEntity.ok(dashboardService.getProductPerformance());
    }

    @GetMapping("/users")
    public ResponseEntity<?> getUserAnalytics() {
        return ResponseEntity.ok(dashboardService.getUserAnalytics());
    }

    @GetMapping("/activity")
    public ResponseEntity<?> getRecentActivity(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(dashboardService.getRecentActivity(limit));
    }

    @GetMapping("/alerts/low-stock")
    public ResponseEntity<?> getLowStockAlerts(
            @RequestParam(defaultValue = "10") int threshold) {
        return ResponseEntity.ok(dashboardService.getLowStockAlerts(threshold));
    }

    @GetMapping("/performance")
    public ResponseEntity<?> getPerformance(
            @RequestParam(defaultValue = "monthly") String period) {
        return ResponseEntity.ok(dashboardService.getPerformanceMetrics(period));
    }

    @GetMapping("/users/engagement")
    public ResponseEntity<?> getUserEngagement() {
        return ResponseEntity.ok(dashboardService.getUserEngagement());
    }

    @GetMapping("/inventory")
    public ResponseEntity<?> getInventorySummary(
            @RequestParam(defaultValue = "10") int threshold) {
        return ResponseEntity.ok(dashboardService.getInventorySummary(threshold));
    }

    @GetMapping("/payment-status")
    public ResponseEntity<?> getPaymentStatus() {
        return ResponseEntity.ok(dashboardService.getPaymentStatus());
    }

    @GetMapping("/comparison")
    public ResponseEntity<?> getMetricsComparison(
            @RequestParam(defaultValue = "month") String period) {
        return ResponseEntity.ok(dashboardService.getMetricsComparison(period));
    }

    @GetMapping("/analytics")
    public ResponseEntity<?> getDashboardAnalytics() {
        return ResponseEntity.ok(dashboardService.getDashboardAnalytics());
    }

    @PostMapping("/export")
    public ResponseEntity<?> exportDashboard(@RequestBody Map<String, Object> request) {
        String format = String.valueOf(request.getOrDefault("format", "csv"));
        String filename = "admin-dashboard-" + LocalDateTime.now().toLocalDate() + "." + format;
        return ResponseEntity.ok(Map.of(
                "url", "/api/v1/admin/dashboard/reports/download/" + filename,
                "filename", filename
        ));
    }

    @PostMapping("/reports/generate")
    public ResponseEntity<?> generateReport(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "reportId", UUID.randomUUID().toString(),
                "downloadUrl", "/api/v1/admin/dashboard/reports/download/generated-report"
        ));
    }
}
