package com.example.demo_project_spring_boot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor  // 🌟 នេះគឺជាចំណុចសំខាន់ (បង្កើត Constructor ទទេ)
@AllArgsConstructor
public class OrderRequestDto {
    private Long userId;
    // អាចបន្ថែម addressId ទីនេះនៅពេលក្រោយ បើអ្នកមានប្រព័ន្ធអាសយដ្ឋាន
    private String couponCode; // User អាចបញ្ជូនកូដមក ឬអត់ក៏បាន (Optional)
}