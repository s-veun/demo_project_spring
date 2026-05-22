package com.example.demo_project_spring_boot.utils;

import com.example.demo_project_spring_boot.dto.ApiResponse;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.Map;

public final class ApiResponseUtils {

    private ApiResponseUtils() {
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(Instant.now().getEpochSecond())
                .build();
    }

    public static <T> ApiResponse<T> success(T data) {
        return success("Operation successful", data);
    }

    public static <T> ApiResponse<T> created(String message, T data) {
        return success(message, data);
    }

    public static <T> Map<String, Object> pageData(Page<T> page) {
        return Map.of(
                "items", page.getContent(),
                "page", page.getNumber(),
                "size", page.getSize(),
                "totalItems", page.getTotalElements(),
                "totalPages", page.getTotalPages(),
                "hasNext", page.hasNext(),
                "hasPrevious", page.hasPrevious()
        );
    }
}

