package com.example.demo_project_spring_boot.repository;

import com.example.demo_project_spring_boot.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderOrderId(Long orderId);
    Optional<Payment> findByTransactionId(String transactionId);
}
