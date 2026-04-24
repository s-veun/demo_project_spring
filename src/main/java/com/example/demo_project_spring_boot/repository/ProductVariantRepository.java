package com.example.demo_project_spring_boot.repository;

import com.example.demo_project_spring_boot.model.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    List<ProductVariant> findByProductProId(Long productId);
    List<ProductVariant> findByProductProIdAndAvailableTrue(Long productId);
    boolean existsByProductProIdAndSku(Long productId, String sku);
}
