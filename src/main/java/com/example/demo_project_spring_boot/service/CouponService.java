package com.example.demo_project_spring_boot.service;

import com.example.demo_project_spring_boot.dto.CouponsRequestDto;
import com.example.demo_project_spring_boot.dto.CouponResponseDto;

import java.util.List;

public interface CouponService {
    CouponResponseDto createCoupon(CouponsRequestDto requestDto);
    List<CouponResponseDto> getAllCoupons();
    CouponResponseDto validateAndGetCoupon(String code); // សំខាន់សម្រាប់ User
    void toggleCouponStatus(Long couponId);
    void deleteCoupon(Long couponId);
}