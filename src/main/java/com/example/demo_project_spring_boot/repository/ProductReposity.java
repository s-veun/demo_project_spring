package com.example.demo_project_spring_boot.repository;

import com.example.demo_project_spring_boot.dto.ProductListDTO;
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

    // ── Get single product (detail page) — entity OK ──────────────────
    @Query("""
        SELECT DISTINCT p FROM Product p
        LEFT JOIN FETCH p.category
        LEFT JOIN FETCH p.images
        WHERE p.proId = :proId
    """)
    Optional<Product> findByIdWithCategory(@Param("proId") Long proId);

    // ── Get all products as DTO — 1 query only ─────────────────────────
    @Query("""
        SELECT new com.example.demo_project_spring_boot.dto.ProductListDTO(
            p.proId, p.proName, p.sku, p.proPrice, p.proBrand,
            c.catName, c.catId,
            (SELECT pi.imageUrl FROM ProductImage pi
             WHERE pi.product.proId = p.proId
             ORDER BY pi.id ASC LIMIT 1),
            p.discount, p.stock, p.available,
            p.tags, p.createdAt, p.updatedAt
        )
        FROM Product p
        LEFT JOIN p.category c
        ORDER BY p.createdAt DESC
    """)
    List<ProductListDTO> findAllAsDTO();

    // ── Search as DTO ──────────────────────────────────────────────────
    @Query("""
        SELECT new com.example.demo_project_spring_boot.dto.ProductListDTO(
            p.proId, p.proName, p.sku, p.proPrice, p.proBrand,
            c.catName, c.catId,
            (SELECT pi.imageUrl FROM ProductImage pi
             WHERE pi.product.proId = p.proId
             ORDER BY pi.id ASC LIMIT 1),
            p.discount, p.stock, p.available,
            p.tags, p.createdAt, p.updatedAt
        )
        FROM Product p
        LEFT JOIN p.category c
        WHERE LOWER(p.proName)     LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(p.proBrand)    LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(c.catName)     LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(p.proDesc)     LIKE LOWER(CONCAT('%', :keyword, '%'))
        ORDER BY p.createdAt DESC
    """)
    List<ProductListDTO> searchAsDTO(@Param("keyword") String keyword);

    // ── findByIdWithCategory — used for update/delete ──────────────────
    @Query("""
        SELECT DISTINCT p FROM Product p
        LEFT JOIN FETCH p.category
        LEFT JOIN FETCH p.images
        WHERE p.proId = :proId
    """)
    Optional<Product> findByIdWithImages(@Param("proId") Long proId);
}