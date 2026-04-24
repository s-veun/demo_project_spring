package com.example.demo_project_spring_boot.repository;

import com.example.demo_project_spring_boot.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser_IdOrderByOrderDateDesc(Long userId);
    boolean existsByUser_IdAndCoupon_CouponId(Long userId, Long couponId);

}
