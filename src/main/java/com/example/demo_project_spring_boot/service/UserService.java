package com.example.demo_project_spring_boot.service;

import com.example.demo_project_spring_boot.dto.ChangePasswordRequest;
import com.example.demo_project_spring_boot.dto.UserProfileResponse;
import com.example.demo_project_spring_boot.model.User;

import java.util.List;

public interface UserService {
    User registerUser(User user);
    void changePassword(String username, ChangePasswordRequest request);
    List<UserProfileResponse> getAllUsers();
    void toggleUserStatus(Long userId); // បិទ/បើក គណនី
    void deleteUser(Long userId);
    UserProfileResponse getUserProfile(Long userId);
    UserProfileResponse getMyProfile(String username);
}