package com.example.demo_project_spring_boot.repository;

import com.example.demo_project_spring_boot.model.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    List<Wishlist> findByUserId(Long userId);
    boolean existsByUserIdAndProductProId(Long userId, Long productId);
    void deleteByUserIdAndProductProId(Long userId, Long productId);
}
