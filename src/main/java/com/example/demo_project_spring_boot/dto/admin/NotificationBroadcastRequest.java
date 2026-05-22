package com.example.demo_project_spring_boot.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class NotificationBroadcastRequest {

    @NotBlank(message = "Notification title is required")
    @Size(max = 150, message = "Title must be at most 150 characters")
    private String title;

    @NotBlank(message = "Notification message is required")
    @Size(max = 4000, message = "Message must be at most 4000 characters")
    private String message;

    @Size(max = 50, message = "Type must be at most 50 characters")
    private String type = "SYSTEM";

    private List<Long> userIds;
}

