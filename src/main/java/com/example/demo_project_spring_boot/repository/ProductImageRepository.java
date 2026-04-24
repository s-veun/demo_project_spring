package com.example.demo_project_spring_boot.repository;

import com.example.demo_project_spring_boot.model.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    List<ProductImage> findByProductProId(Long productId);
    void deleteByProductProId(Long productId);
}
