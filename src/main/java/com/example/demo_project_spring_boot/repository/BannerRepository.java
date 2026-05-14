package com.example.demo_project_spring_boot.repository;

import com.example.demo_project_spring_boot.model.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Long> {

    /**
     * Find all active banners sorted by position order
     */
    @Query("SELECT b FROM Banner b WHERE b.isActive = true ORDER BY b.positionOrder ASC")
    List<Banner> findAllActiveBannersSortedByPosition();

    /**
     * Find all banners (including inactive) sorted by position order
     */
    @Query("SELECT b FROM Banner b ORDER BY b.positionOrder ASC, b.createdAt DESC")
    List<Banner> findAllBannersSortedByPosition();

    /**
     * Find banner by ID
     */
    Optional<Banner> findById(Long bannerId);

    /**
     * Check if a banner with specific position order exists
     */
    boolean existsByPositionOrder(Integer positionOrder);

    /**
     * Find the next position order for a new banner
     */
    @Query(value = "SELECT COALESCE(MAX(position_order), 0) + 1 FROM banners", nativeQuery = true)
    Integer getNextPositionOrder();
}

