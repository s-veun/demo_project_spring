package com.example.demo_project_spring_boot.controller;

import com.example.demo_project_spring_boot.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
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
}
