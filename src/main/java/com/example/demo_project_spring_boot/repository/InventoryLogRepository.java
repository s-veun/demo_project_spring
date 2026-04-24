package com.example.demo_project_spring_boot.repository;

import com.example.demo_project_spring_boot.model.InventoryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {
    List<InventoryLog> findByProductProId(Long productId);
    List<InventoryLog> findByProductProIdOrderByCreatedAtDesc(Long productId);
}
