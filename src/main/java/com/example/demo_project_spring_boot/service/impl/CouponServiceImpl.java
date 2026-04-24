package com.example.demo_project_spring_boot.service.impl;

import com.example.demo_project_spring_boot.dto.CouponsRequestDto;
import com.example.demo_project_spring_boot.dto.CouponResponseDto;
import com.example.demo_project_spring_boot.model.Coupon;
import com.example.demo_project_spring_boot.repository.CouponRepository;
import com.example.demo_project_spring_boot.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;

    @Override
    public CouponResponseDto createCoupon(CouponsRequestDto requestDto) {
        // ១. ឆែកមើលថាកូដនេះមានគេបង្កើតហើយឬនៅ (Code ត្រូវតែ Unique)
        if (couponRepository.existsByCode(requestDto.getCode().toUpperCase())) {
            throw new RuntimeException("Coupon code នេះមានរួចហើយ! សូមបង្កើតកូដផ្សេង។");
        }

        Coupon coupon = new Coupon();
        // រក្សាទុកកូដជាអក្សរធំទាំងអស់ (Uppercase) ដើម្បីងាយស្រួលផ្ទៀងផ្ទាត់
        coupon.setCode(requestDto.getCode().toUpperCase());
        coupon.setDiscountValue(requestDto.getDiscountValue());
        coupon.setExpiryDate(requestDto.getExpiryDate());
        
        // បើគេមិនបញ្ជូន active មក យើងឱ្យវា = true ជាលំនាំដើម
        coupon.setActive(requestDto.getActive() != null ? requestDto.getActive() : true);

        Coupon savedCoupon = couponRepository.save(coupon);
        return mapToDto(savedCoupon);
    }

    @Override
    public List<CouponResponseDto> getAllCoupons() {
        return couponRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // 🌟 មុខងារនេះសំខាន់បំផុត សម្រាប់ពេល User វាយបញ្ជូលកូដបញ្ចុះតម្លៃ
    @Override
    public CouponResponseDto validateAndGetCoupon(String code) {
        Coupon coupon = couponRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new RuntimeException("រកមិនឃើញកូដបញ្ចុះតម្លៃនេះទេ! (Invalid Coupon)"));

        // ១. ឆែកមើលថាតើ Admin បានបិទកូដនេះហើយឬនៅ
        if (!coupon.getActive()) {
            throw new RuntimeException("កូដបញ្ចុះតម្លៃនេះត្រូវបានបិទលែងឱ្យប្រើប្រាស់ហើយ! (Coupon is inactive)");
        }

        // ២. ឆែកមើលថ្ងៃផុតកំណត់ (Expiry Date)
        if (coupon.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("កូដបញ្ចុះតម្លៃនេះបានផុតកំណត់ហើយ! (Coupon has expired)");
        }

        return mapToDto(coupon);
    }

    @Override
    public void toggleCouponStatus(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new RuntimeException("រកមិនឃើញ Coupon ID: " + couponId));
        
        // ប្ដូរពីពិតទៅមិនពិត ឬមិនពិតទៅពិត (Toggle)
        coupon.setActive(!coupon.getActive());
        couponRepository.save(coupon);
    }

    @Override
    public void deleteCoupon(Long couponId) {
        if (!couponRepository.existsById(couponId)) {
            throw new RuntimeException("រកមិនឃើញ Coupon ID: " + couponId);
        }
        couponRepository.deleteById(couponId);
    }

    private CouponResponseDto mapToDto(Coupon coupon) {
        CouponResponseDto dto = new CouponResponseDto();
        dto.setCouponId(coupon.getCouponId()); // ឥឡូវនេះវានឹងលែង Error ទៀតហើយ
        dto.setCode(coupon.getCode());
        dto.setDiscountValue(coupon.getDiscountValue());
        dto.setExpiryDate(coupon.getExpiryDate());
        dto.setActive(coupon.getActive());
        return dto;
    }
}