package com.example.demo_project_spring_boot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SingleImageUploadRequest {

    @Schema(type = "string", format = "binary", description = "Image file to upload")
    private MultipartFile file;

    @Schema(type = "string", defaultValue = "uploads", description = "Destination folder")
    private String folder = "uploads";
}