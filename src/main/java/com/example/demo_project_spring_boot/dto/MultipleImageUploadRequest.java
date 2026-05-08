package com.example.demo_project_spring_boot.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class MultipleImageUploadRequest {
    private MultipartFile[] files;
    private String folder = "uploads";
}