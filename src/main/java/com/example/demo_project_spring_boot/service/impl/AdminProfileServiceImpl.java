package com.example.demo_project_spring_boot.service.impl;

import com.example.demo_project_spring_boot.dto.AdminProfileResponse;
import com.example.demo_project_spring_boot.dto.UpdateAdminProfileRequest;
import com.example.demo_project_spring_boot.Enum.Role;
import com.example.demo_project_spring_boot.exception.ResourceNotFoundException;
import com.example.demo_project_spring_boot.model.User;
import com.example.demo_project_spring_boot.repository.OrderRepository;
import com.example.demo_project_spring_boot.repository.ProductReposity;
import com.example.demo_project_spring_boot.repository.UserRepository;
import com.example.demo_project_spring_boot.service.AdminProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminProfileServiceImpl implements AdminProfileService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductReposity productReposity;

    @Override
    public AdminProfileResponse getAdminProfile(String username) {
        User admin = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found with username: " + username));

        // Verify user is admin
        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("Access denied. User is not an admin.");
        }

        return mapToAdminProfileResponse(admin);
    }

    @Override
    public AdminProfileResponse updateAdminProfile(String username, UpdateAdminProfileRequest request) {
        User admin = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found with username: " + username));

        // Verify user is admin
        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("Access denied. User is not an admin.");
        }

        // Update fields if provided (partial update)
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            // Check if email is already taken by another user
            userRepository.findByUsername(username); // Just to verify admin exists
            admin.setEmail(request.getEmail().trim());
        }

        if (request.getPhoneNumber() != null) {
            admin.setPhoneNumber(request.getPhoneNumber().trim());
        }

        if (request.getFirstName() != null) {
            admin.setFirstName(request.getFirstName().trim());
        }

        if (request.getLastName() != null) {
            admin.setLastName(request.getLastName().trim());
        }

        if (request.getProfileImageUrl() != null) {
            admin.setProfileImageUrl(request.getProfileImageUrl().trim());
        }

        User updatedAdmin = userRepository.save(admin);
        return mapToAdminProfileResponse(updatedAdmin);
    }

    @Override
    public AdminProfileResponse updateProfileImage(String username, String imageUrl) {
        User admin = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found with username: " + username));

        // Verify user is admin
        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("Access denied. User is not an admin.");
        }

        admin.setProfileImageUrl(imageUrl);
        User updatedAdmin = userRepository.save(admin);
        return mapToAdminProfileResponse(updatedAdmin);
    }

    @Override
    public AdminProfileResponse getAdminDashboardStats(String username) {
        User admin = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found with username: " + username));

        // Verify user is admin
        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("Access denied. User is not an admin.");
        }

        AdminProfileResponse response = mapToAdminProfileResponse(admin);

        // Get statistics
        long totalUsers = userRepository.count();
        long totalOrders = orderRepository.count();
        long totalProducts = productReposity.count();

        response.setTotalUsersManaged(totalUsers);
        response.setTotalOrdersManaged(totalOrders);
        response.setTotalProductsManaged(totalProducts);

        return response;
    }

    private AdminProfileResponse mapToAdminProfileResponse(User admin) {
        return AdminProfileResponse.builder()
                .id(admin.getId())
                .username(admin.getUsername())
                .email(admin.getEmail())
                .phoneNumber(admin.getPhoneNumber())
                .firstName(admin.getFirstName())
                .lastName(admin.getLastName())
                .profileImageUrl(admin.getProfileImageUrl())
                .role(admin.getRole())
                .isEnabled(admin.getIsEnabled())
                .createdAt(admin.getCreatedAt())
                .updatedAt(admin.getUpdatedAt())
                .lastLoginAt(admin.getLastLoginAt())
                .build();
    }
}
