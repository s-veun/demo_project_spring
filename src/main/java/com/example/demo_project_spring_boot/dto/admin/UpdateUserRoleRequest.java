package com.example.demo_project_spring_boot.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import com.example.demo_project_spring_boot.Enum.Role;

@Data
public class UpdateUserRoleRequest {

    @NotNull(message = "Role is required")
    private Role role;
}

