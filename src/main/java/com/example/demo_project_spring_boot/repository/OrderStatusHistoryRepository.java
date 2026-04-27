package com.example.demo_project_spring_boot.repository;

import com.example.demo_project_spring_boot.Enum.OrderStatus;
import com.example.demo_project_spring_boot.model.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {

    List<OrderStatusHistory> findByOrderOrderIdOrderByCreatedAtDesc(Long orderId);

    Optional<OrderStatusHistory> findTopByOrderOrderIdOrderByCreatedAtDesc(Long orderId);

    List<OrderStatusHistory> findByOrderOrderIdAndStatus(Long orderId, com.example.demo_project_spring_boot.Enum.OrderStatus status);

    @Query("SELECT COUNT(osh) FROM OrderStatusHistory osh WHERE osh.status = :status")
    Long countByStatus(@Param("status") com.example.demo_project_spring_boot.Enum.OrderStatus status);

    @Query("SELECT osh.status, COUNT(osh) FROM OrderStatusHistory osh GROUP BY osh.status")
    List<Object[]> countByStatusGrouped();
}
