package com.example.demo_project_spring_boot.dto.admin.settings;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UploadAssetResponse {
    private String assetType;
    private String url;
    private String publicId;
}

