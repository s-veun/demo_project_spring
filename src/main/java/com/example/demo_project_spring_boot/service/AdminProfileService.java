package com.example.demo_project_spring_boot.service;

import com.example.demo_project_spring_boot.dto.AdminProfileResponse;
import com.example.demo_project_spring_boot.dto.UpdateAdminProfileRequest;

public interface AdminProfileService {
    
    // Get current admin profile
    AdminProfileResponse getAdminProfile(String username);
    
    // Update admin profile
    AdminProfileResponse updateAdminProfile(String username, UpdateAdminProfileRequest request);
    
    // Update admin profile image
    AdminProfileResponse updateProfileImage(String username, String imageUrl);
    
    // Get admin dashboard statistics
    AdminProfileResponse getAdminDashboardStats(String username);
}
