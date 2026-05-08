package com.example.demo_project_spring_boot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@Schema(name = "MultipleFileUpload")   // ← link ទៅ schema ក្នុង OpenAPIConfig
public class MultipleImageUploadRequest {

    @Schema(
            type = "array",
            format = "binary",
            description = "Image files to upload",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private MultipartFile[] files;

    @Schema(
            type = "string",
            defaultValue = "uploads",
            description = "Destination folder on Cloudinary"
    )
    private String folder = "uploads";
}