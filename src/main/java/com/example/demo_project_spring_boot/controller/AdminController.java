package com.example.demo_project_spring_boot.controller;

import com.example.demo_project_spring_boot.dto.UserProfileResponse;
import com.example.demo_project_spring_boot.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
// ទាមទារឱ្យមាន Role ជា ADMIN ទើបហៅ API ក្នុង Controller នេះបាន
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;

    // ១. ទាញយកបញ្ជី User ទាំងអស់
    @GetMapping
    public ResponseEntity<List<UserProfileResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // ២. បិទ ឬ បើកគណនី User (Disable / Enable)
    @PutMapping("/{userId}/toggle-status")
    public ResponseEntity<String> toggleUserStatus(@PathVariable Long userId) {
        userService.toggleUserStatus(userId);
        return ResponseEntity.ok("User status toggled successfully");
    }

    // ៣. លុប User ចោល
    @DeleteMapping("/{userId}")
    public ResponseEntity<String> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok("User deleted successfully with ID: " + userId);
    }
}