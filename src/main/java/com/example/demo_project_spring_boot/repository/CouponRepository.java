package com.example.demo_project_spring_boot.repository;

import com.example.demo_project_spring_boot.model.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    
    // ស្វែងរក Coupon តាមឈ្មោះ Code (ប្រើសម្រាប់ពេល User បញ្ចូលកូដ)
    Optional<Coupon> findByCode(String code);
    
    // ឆែកមើលថាកូដនេះមានអ្នកបង្កើតជាន់គ្នាឬអត់
    boolean existsByCode(String code);
}