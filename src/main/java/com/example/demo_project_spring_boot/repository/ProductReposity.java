package com.example.demo_project_spring_boot.repository;

import com.example.demo_project_spring_boot.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductReposity extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Boolean existsByProName(String proName);

    @Query("""
        SELECT DISTINCT p FROM Product p
        LEFT JOIN FETCH p.category
        LEFT JOIN FETCH p.images
        WHERE p.proId = :proId
    """)
    Optional<Product> findByIdWithCategory(@Param("proId") Long proId);

    @Query("""
        SELECT DISTINCT p FROM Product p
        LEFT JOIN FETCH p.category
        LEFT JOIN FETCH p.images
    """)
    List<Product> findAllWithCategory();

    @Query("""
        SELECT p FROM Product p
        WHERE LOWER(p.proName) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(p.proBrand) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(p.category.catName) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(p.proDesc) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
    List<Product> searchProducts(@Param("keyword") String keyword);

    @Query("""
        SELECT DISTINCT p FROM Product p
        LEFT JOIN FETCH p.category
        LEFT JOIN FETCH p.images
        WHERE LOWER(p.proName) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(p.proBrand) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(p.category.catName) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(p.proDesc) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
    List<Product> searchProductsWithCategory(@Param("keyword") String keyword);
}