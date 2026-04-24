package com.example.demo_project_spring_boot.model;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long proId;

    @Column(unique = true, nullable = false)
    private String proName;
    
    @Column(unique = true)
    private String sku;

    @Column(columnDefinition = "TEXT")
    private String proDesc;

    private BigDecimal proPrice;
    private String proBrand;
    
    private Double weight;
    
    private Double length;
    
    private Double width;
    
    private Double height;

    // Popularity tracking
    @Builder.Default
    private Long viewCount = 0L;
    
    @Builder.Default
    private Long purchaseCount = 0L;
    
    private Double popularityScore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cat_id")
    private Category category;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date releaseDate;

    private Boolean available;
    private Double rating;
    private Double discount;
    private Integer stock;
    private String tags;
    private Boolean favourite;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();
    
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductVariant> variants = new ArrayList<>();
    
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<InventoryLog> inventoryLogs = new ArrayList<>();
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.images == null) this.images = new ArrayList<>();
        if (this.reviews == null) this.reviews = new ArrayList<>();
        if (this.variants == null) this.variants = new ArrayList<>();
        if (this.inventoryLogs == null) this.inventoryLogs = new ArrayList<>();
        if (this.available == null) this.available = true;
        if (this.favourite == null) this.favourite = false;
        if (this.rating == null) this.rating = 0.0;
        if (this.stock == null) this.stock = 0;
        if (this.releaseDate == null) this.releaseDate = new Date();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

}