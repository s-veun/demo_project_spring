package com.example.demo_project_spring_boot.controller.admin;

import com.example.demo_project_spring_boot.Enum.Role;
import com.example.demo_project_spring_boot.dto.ApiResponse;
import com.example.demo_project_spring_boot.dto.admin.NotificationBroadcastRequest;
import com.example.demo_project_spring_boot.dto.admin.UpdateUserRoleRequest;
import com.example.demo_project_spring_boot.dto.admin.UpdateUserStatusRequest;
import com.example.demo_project_spring_boot.exception.ResourceNotFoundException;
import com.example.demo_project_spring_boot.model.Notification;
import com.example.demo_project_spring_boot.model.Review;
import com.example.demo_project_spring_boot.model.User;
import com.example.demo_project_spring_boot.repository.NotificationRepository;
import com.example.demo_project_spring_boot.repository.ReviewRepository;
import com.example.demo_project_spring_boot.repository.UserRepository;
import com.example.demo_project_spring_boot.utils.ApiResponseUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Operations", description = "Extended admin management APIs for dashboard integration")
public class AdminOperationsController {

    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationRepository notificationRepository;

    @GetMapping("/users/paged")
    @Operation(summary = "Get users with pagination, filtering and sorting")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUsersPaged(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = "asc".equalsIgnoreCase(sortDir) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), sort);

        Page<User> users = userRepository.searchUsers(keyword, role, enabled, pageable);
        List<Map<String, Object>> items = users.getContent().stream().map(user -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", user.getId());
            row.put("username", user.getUsername());
            row.put("email", user.getEmail());
            row.put("role", user.getRole().name());
            row.put("enabled", Boolean.TRUE.equals(user.getIsEnabled()));
            row.put("createdAt", user.getCreatedAt());
            row.put("lastLoginAt", user.getLastLoginAt());
            return row;
        }).toList();

        return ResponseEntity.ok(ApiResponseUtils.success("Users fetched successfully", Map.of(
                "items", items,
                "page", users.getNumber(),
                "size", users.getSize(),
                "totalItems", users.getTotalElements(),
                "totalPages", users.getTotalPages(),
                "hasNext", users.hasNext(),
                "hasPrevious", users.hasPrevious()
        )));
    }

    @PatchMapping("/users/{userId}/role")
    @Operation(summary = "Update user role")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateUserRole(
            @PathVariable Long userId,
            @RequestBody @Valid UpdateUserRoleRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setRole(request.getRole());
        userRepository.save(user);

        return ResponseEntity.ok(ApiResponseUtils.success("User role updated successfully", Map.of(
                "userId", user.getId(),
                "role", user.getRole().name()
        )));
    }

    @PatchMapping("/users/{userId}/status")
    @Operation(summary = "Activate or deactivate user account")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateUserStatus(
            @PathVariable Long userId,
            @RequestBody @Valid UpdateUserStatusRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setIsEnabled(request.getEnabled());
        userRepository.save(user);

        return ResponseEntity.ok(ApiResponseUtils.success("User status updated successfully", Map.of(
                "userId", user.getId(),
                "enabled", Boolean.TRUE.equals(user.getIsEnabled())
        )));
    }

    @GetMapping("/reviews")
    @Operation(summary = "Get all reviews with pagination and filters")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReviews(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer rating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = "asc".equalsIgnoreCase(sortDir) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), sort);
        Page<Review> reviews = reviewRepository.searchForAdmin(keyword, rating, pageable);

        List<Map<String, Object>> items = reviews.getContent().stream().map(review -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("reviewId", review.getReviewId());
            row.put("rating", review.getRating());
            row.put("comment", review.getComment());
            row.put("createdAt", review.getCreatedAt());
            row.put("username", review.getUser() != null ? review.getUser().getUsername() : "");
            row.put("productId", review.getProduct() != null ? review.getProduct().getProId() : null);
            row.put("productName", review.getProduct() != null ? review.getProduct().getProName() : "");
            return row;
        }).toList();

        return ResponseEntity.ok(ApiResponseUtils.success("Reviews fetched successfully", Map.of(
                "items", items,
                "page", reviews.getNumber(),
                "size", reviews.getSize(),
                "totalItems", reviews.getTotalElements(),
                "totalPages", reviews.getTotalPages(),
                "hasNext", reviews.hasNext(),
                "hasPrevious", reviews.hasPrevious()
        )));
    }

    @DeleteMapping("/reviews/{reviewId}")
    @Operation(summary = "Delete an inappropriate review")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteReview(@PathVariable Long reviewId) {
        if (!reviewRepository.existsById(reviewId)) {
            throw new ResourceNotFoundException("Review not found");
        }
        reviewRepository.deleteById(reviewId);
        return ResponseEntity.ok(ApiResponseUtils.success("Review deleted successfully", Map.of("reviewId", reviewId)));
    }

    @GetMapping("/reviews/stats")
    @Operation(summary = "Get review statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReviewStats() {
        Map<String, Object> data = Map.of(
                "totalReviews", reviewRepository.count(),
                "averageRating", reviewRepository.averageRating(),
                "ratings", Map.of(
                        "1", reviewRepository.countByRating(1),
                        "2", reviewRepository.countByRating(2),
                        "3", reviewRepository.countByRating(3),
                        "4", reviewRepository.countByRating(4),
                        "5", reviewRepository.countByRating(5)
                )
        );
        return ResponseEntity.ok(ApiResponseUtils.success("Review statistics fetched successfully", data));
    }

    @PostMapping("/notifications/broadcast")
    @Operation(summary = "Broadcast notification to all users or selected users")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> broadcastNotification(
            @RequestBody @Valid NotificationBroadcastRequest request
    ) {
        List<User> recipients;
        if (request.getUserIds() == null || request.getUserIds().isEmpty()) {
            recipients = userRepository.findAll();
        } else {
            recipients = userRepository.findAllById(request.getUserIds());
        }

        if (recipients.isEmpty()) {
            throw new ResourceNotFoundException("No target users found for notification");
        }

        String normalizedType = request.getType() == null || request.getType().isBlank()
                ? "SYSTEM"
                : request.getType().trim().toUpperCase(Locale.ROOT);

        List<Notification> notifications = new ArrayList<>(recipients.size());
        for (User user : recipients) {
            notifications.add(Notification.builder()
                    .user(user)
                    .title(request.getTitle().trim())
                    .message(request.getMessage().trim())
                    .type(normalizedType)
                    .isRead(false)
                    .build());
        }
        notificationRepository.saveAll(notifications);

        return ResponseEntity.ok(ApiResponseUtils.success("Notification sent successfully", Map.of(
                "recipientCount", notifications.size(),
                "type", normalizedType,
                "createdAt", Instant.now().toString()
        )));
    }

    @GetMapping("/notifications/history")
    @Operation(summary = "Get notification history")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNotificationHistory(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by("createdAt").descending());
        Page<Notification> notifications = (type == null || type.isBlank())
                ? notificationRepository.findAllByOrderByCreatedAtDesc(pageable)
                : notificationRepository.findByTypeIgnoreCaseOrderByCreatedAtDesc(type, pageable);

        List<Map<String, Object>> items = notifications.getContent().stream().map(notification -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("notificationId", notification.getNotificationId());
            row.put("title", notification.getTitle());
            row.put("message", notification.getMessage());
            row.put("type", notification.getType());
            row.put("isRead", notification.getIsRead());
            row.put("createdAt", notification.getCreatedAt());
            row.put("userId", notification.getUser() != null ? notification.getUser().getId() : null);
            row.put("username", notification.getUser() != null ? notification.getUser().getUsername() : "");
            return row;
        }).toList();

        return ResponseEntity.ok(ApiResponseUtils.success("Notification history fetched successfully", Map.of(
                "items", items,
                "page", notifications.getNumber(),
                "size", notifications.getSize(),
                "totalItems", notifications.getTotalElements(),
                "totalPages", notifications.getTotalPages(),
                "hasNext", notifications.hasNext(),
                "hasPrevious", notifications.hasPrevious()
        )));
    }

    @GetMapping("/system/health")
    @Operation(summary = "Get lightweight system health stats for admin dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemHealth() {
        Map<String, Object> data = Map.of(
                "status", "UP",
                "users", userRepository.count(),
                "reviews", reviewRepository.count(),
                "notifications", notificationRepository.count(),
                "timestamp", Instant.now().toString()
        );
        return ResponseEntity.ok(ApiResponseUtils.success("System health fetched successfully", data));
    }
}


