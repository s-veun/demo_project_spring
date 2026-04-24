package com.example.demo_project_spring_boot.repository;

import com.example.demo_project_spring_boot.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserId(Long userId);
    List<Notification> findByUserIdAndIsReadFalse(Long userId);
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    Long countByUserIdAndIsReadFalse(Long userId);
}
