package com.example.demo_project_spring_boot.controller;

import com.example.demo_project_spring_boot.dto.UserProfileResponse;
import com.example.demo_project_spring_boot.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin management APIs")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final UserService userService;

    // ១. ទាញយកបញ្ជី User ទាំងអស់
    @GetMapping("/users")
    @Operation(summary = "Get all users")
    public ResponseEntity<List<UserProfileResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // ២. បិទ ឬ បើកគណនី User
    @PutMapping("/users/{userId}/toggle-status")
    @Operation(summary = "Enable or disable user account")
    public ResponseEntity<Map<String, String>> toggleUserStatus(@PathVariable Long userId) {
        userService.toggleUserStatus(userId);
        return ResponseEntity.ok(Map.of("message", "User status toggled successfully"));
    }

    // ៣. លុប User
    @DeleteMapping("/users/{userId}")
    @Operation(summary = "Delete user by ID")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully with ID: " + userId));
    }

    // ✅ ៤. Promote User → ADMIN
    @PutMapping("/users/{userId}/make-admin")
    @Operation(summary = "Promote user to ADMIN role")
    public ResponseEntity<Map<String, String>> makeAdmin(@PathVariable Long userId) {
        userService.changeUserRole(userId, "ADMIN");
        return ResponseEntity.ok(Map.of("message", "User promoted to ADMIN successfully"));
    }

    // ✅ ៥. Demote ADMIN → USER
    @PutMapping("/users/{userId}/make-user")
    @Operation(summary = "Demote user to USER role")
    public ResponseEntity<Map<String, String>> makeUser(@PathVariable Long userId) {
        userService.changeUserRole(userId, "USER");
        return ResponseEntity.ok(Map.of("message", "User demoted to USER successfully"));
    }
}