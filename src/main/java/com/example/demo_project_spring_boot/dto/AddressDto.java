package com.example.demo_project_spring_boot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AddressDto {
    private Long addressId;
    private String fullName;
    private String phoneNumber;
    private String city;
    private String district;
    private String detailsAddress;
    private Long userId;
}
