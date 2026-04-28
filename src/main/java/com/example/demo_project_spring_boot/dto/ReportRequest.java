package com.example.demo_project_spring_boot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReportRequest {
    private String reportType; // "sales", "users", "products", "revenue"
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String groupBy; // "day", "week", "month", "year"
    private String format; // "json", "csv"
}
