package com.example.demo_project_spring_boot.repository;

import com.example.demo_project_spring_boot.Enum.OrderStatus;
import com.example.demo_project_spring_boot.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser_IdOrderByOrderDateDesc(Long userId);
    boolean existsByUser_IdAndCoupon_CouponId(Long userId, Long couponId);
    List<Order> findByStatus(OrderStatus status);

    // ✅ Fixed: Native Query ជំនួស JPQL
    @Query(value = "SELECT o.* FROM tbl_orders o " +
            "JOIN users u ON o.user_id = u.id " +
            "WHERE " +
            "( CAST(:keyword AS TEXT) IS NULL OR " +
            "  CAST(o.order_id AS TEXT) LIKE CONCAT('%', :keyword, '%') OR " +
            "  LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "  LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) ) AND " +
            "( CAST(:status AS TEXT) IS NULL OR o.status = :status ) AND " +
            "( CAST(:startDate AS TIMESTAMP) IS NULL OR o.order_date >= CAST(:startDate AS TIMESTAMP) ) AND " +
            "( CAST(:endDate AS TIMESTAMP) IS NULL OR o.order_date <= CAST(:endDate AS TIMESTAMP) ) AND " +
            "( CAST(:userId AS BIGINT) IS NULL OR o.user_id = CAST(:userId AS BIGINT) )",
            countQuery = "SELECT COUNT(*) FROM tbl_orders o " +
                    "JOIN users u ON o.user_id = u.id " +
                    "WHERE " +
                    "( CAST(:keyword AS TEXT) IS NULL OR " +
                    "  CAST(o.order_id AS TEXT) LIKE CONCAT('%', :keyword, '%') OR " +
                    "  LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "  LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) ) AND " +
                    "( CAST(:status AS TEXT) IS NULL OR o.status = :status ) AND " +
                    "( CAST(:startDate AS TIMESTAMP) IS NULL OR o.order_date >= CAST(:startDate AS TIMESTAMP) ) AND " +
                    "( CAST(:endDate AS TIMESTAMP) IS NULL OR o.order_date <= CAST(:endDate AS TIMESTAMP) ) AND " +
                    "( CAST(:userId AS BIGINT) IS NULL OR o.user_id = CAST(:userId AS BIGINT) )",
            nativeQuery = true)
    Page<Order> searchOrders(
            @Param("keyword") String keyword,
            @Param("status") String status,       // ✅ ប្តូរជា String
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("userId") Long userId,
            Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate")
    List<Order> findOrdersByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE " +
            "o.status != :excludedStatus AND " +
            "o.orderDate BETWEEN :startDate AND :endDate")
    BigDecimal calculateRevenueByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("excludedStatus") OrderStatus excludedStatus);

    long countByStatus(OrderStatus status);

    @Query("SELECT o FROM Order o ORDER BY o.orderDate DESC")
    Page<Order> findRecentOrders(Pageable pageable);
}