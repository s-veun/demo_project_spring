package com.example.demo_project_spring_boot.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity(name = "categories")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long catId;

    @Column(nullable = false, unique = true)
    private String catName;
    
    @Column(columnDefinition = "TEXT")
    private String catDesc;
    
    private String imageUrl;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @JsonIgnore
    private Category parentCategory;
    
    @OneToMany(mappedBy = "parentCategory", cascade = CascadeType.ALL)
    @JsonIgnore
    @Builder.Default
    private List<Category> subcategories = new ArrayList<>();

    @OneToMany(mappedBy = "category")
    @JsonIgnore
    @Builder.Default
    private List<Product> products = new ArrayList<>();
    
    private Boolean isActive;
    
    private Integer sortOrder;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.isActive == null) this.isActive = true;
        if (this.sortOrder == null) this.sortOrder = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @JsonCreator
    public Category(String catName) {
        this.catName = catName;
    }
}