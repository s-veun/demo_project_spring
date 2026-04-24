package com.example.demo_project_spring_boot.dto;

import com.example.demo_project_spring_boot.Enum.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String username;
    private Role role;
    private List<AddressDto> addresses;
}
