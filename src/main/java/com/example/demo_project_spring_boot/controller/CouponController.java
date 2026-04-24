package com.example.demo_project_spring_boot.controller;

import com.example.demo_project_spring_boot.dto.CouponsRequestDto;
import com.example.demo_project_spring_boot.dto.CouponResponseDto;
import com.example.demo_project_spring_boot.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    // ==========================================
    // ផ្នែកសម្រាប់ USER (ការអនុវត្តកូដបញ្ចុះតម្លៃ)
    // ==========================================

    // ឆែកមើលថាកូដហ្នឹងប្រើបានឬអត់ (បញ្ចូល Code តាម URL parameters)
    @GetMapping("/validate")
    public ResponseEntity<?> validateCoupon(@RequestParam String code) {
        try {
            CouponResponseDto response = couponService.validateAndGetCoupon(code);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ==========================================
    // ផ្នែកសម្រាប់ ADMIN (ការគ្រប់គ្រង)
    // ==========================================

    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createCoupon(@RequestBody CouponsRequestDto requestDto) {
        try {
            CouponResponseDto response = couponService.createCoupon(requestDto);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CouponResponseDto>> getAllCoupons() {
        return ResponseEntity.ok(couponService.getAllCoupons());
    }

    @PutMapping("/{couponId}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> toggleCoupon(@PathVariable Long couponId) {
        try {
            couponService.toggleCouponStatus(couponId);
            return ResponseEntity.ok("ស្ថានភាពកូដបញ្ចុះតម្លៃត្រូវបានកែប្រែជោគជ័យ!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{couponId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteCoupon(@PathVariable Long couponId) {
        try {
            couponService.deleteCoupon(couponId);
            return ResponseEntity.ok("លុបកូដបញ្ចុះតម្លៃជោគជ័យ!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}