package com.example.demo_project_spring_boot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UploadImageResponse {
    private String username;
    private String email;
    private String imageUrl;
    private OffsetDateTime uploadTimestamp;
}

