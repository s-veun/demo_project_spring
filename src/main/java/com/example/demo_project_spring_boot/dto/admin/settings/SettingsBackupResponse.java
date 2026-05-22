package com.example.demo_project_spring_boot.dto.admin.settings;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class SettingsBackupResponse {
    private String tenant;
    private String locale;
    private Instant exportedAt;
    private Map<String, Map<String, Object>> categories;
}

