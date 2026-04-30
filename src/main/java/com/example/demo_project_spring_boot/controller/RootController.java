package com.example.demo_project_spring_boot.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class RootController {
    @GetMapping("/")
    public Map<String, Object> root(HttpServletRequest request) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "ok");
        response.put("message", "demo_project_spring_boot is running");
        response.put("path", request.getRequestURI());
        response.put("timestamp", LocalDateTime.now().toString());
        return response;
    }
}

