package com.example.demo_project_spring_boot.dto;

import com.example.demo_project_spring_boot.Enum.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserSearchRequest {
    private String keyword;
    private Role role;
    private Boolean isEnabled;
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortOrder; // "asc" or "desc"
}
