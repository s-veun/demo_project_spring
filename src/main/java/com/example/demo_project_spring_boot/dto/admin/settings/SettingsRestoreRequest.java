package com.example.demo_project_spring_boot.dto.admin.settings;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class SettingsRestoreRequest {

    @NotNull
    private Map<String, Map<String, Object>> categories;

    private boolean overwriteExisting = true;
}

