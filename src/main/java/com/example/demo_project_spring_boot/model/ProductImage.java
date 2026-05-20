package com.example.demo_project_spring_boot.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "product_images")
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================================
    // CLOUDINARY INFO
    // =========================================

    @Column(nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

    @Column(nullable = true)
    private String publicId;

    // =========================================
    // IMAGE SETTINGS
    // =========================================

    // thumbnail image for product card
    @Builder.Default
    private Boolean thumbnail = false;

    // image sort order
    @Builder.Default
    private Integer sortOrder = 0;

    // alt text for SEO/frontend
    private String altText;

    // =========================================
    // RELATIONSHIP
    // =========================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonBackReference
    private Product product;
}