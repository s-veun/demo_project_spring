package com.example.demo_project_spring_boot.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserAddressResponse {
    private Long id;
    private String fullName;
    private String phoneNumber;
    private String country;
    private String city;
    private String state;
    private String district;
    private String postalCode;
    private String addressLine1;
    private String addressLine2;
    private String detailsAddress;
    private Boolean isDefault;
}

