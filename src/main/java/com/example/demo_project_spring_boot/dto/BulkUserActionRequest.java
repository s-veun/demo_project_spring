package com.example.demo_project_spring_boot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BulkUserActionRequest {
    private List<Long> userIds;
    private String action; // "enable", "disable", "delete", "make-admin", "make-user"
}
