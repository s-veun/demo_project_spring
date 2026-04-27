package com.example.demo_project_spring_boot.controller;

import com.example.demo_project_spring_boot.config.JwtService;
import com.example.demo_project_spring_boot.dto.LoginRequest;
import com.example.demo_project_spring_boot.dto.RegisterRequest;
import com.example.demo_project_spring_boot.dto.UserProfileResponse;
import com.example.demo_project_spring_boot.model.User;
import com.example.demo_project_spring_boot.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin management APIs")
public class AdminController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    // ✅ ១. Admin Register — Public
    @PostMapping("/register")
    @Operation(summary = "Register new ADMIN account")
    public ResponseEntity<?> registerAdmin(@RequestBody RegisterRequest request) {
        try {
            User user = new User();
            user.setUsername(request.getUsername());
            user.setPassword(request.getPassword());
            user.setEmail(request.getEmail());
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setPhoneNumber(request.getPhoneNumber());

            User registeredAdmin = userService.registerAdmin(user);
            registeredAdmin.setPassword(null);
            return new ResponseEntity<>(registeredAdmin, HttpStatus.CREATED);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ ២. Admin Login — Public
    @PostMapping("/login")
    @Operation(summary = "Admin login — returns JWT token")
    public ResponseEntity<?> loginAdmin(@RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            UserDetails userDetails = userDetailsService
                    .loadUserByUsername(request.getUsername());

            boolean isAdmin = userDetails.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if (!isAdmin) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied. Admin only!"));
            }

            String token = jwtService.generateToken(userDetails);

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("username", userDetails.getUsername());
            response.put("role", "ADMIN");
            response.put("message", "Admin logged in successfully");

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid username or password"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ៣. Get All Users — ADMIN only
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get all users")
    public ResponseEntity<List<UserProfileResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // ៤. Toggle User Status — ADMIN only
    @PutMapping("/users/{userId}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Enable or disable user account")
    public ResponseEntity<Map<String, String>> toggleUserStatus(
            @PathVariable Long userId) {
        userService.toggleUserStatus(userId);
        return ResponseEntity.ok(
                Map.of("message", "User status toggled successfully"));
    }

    // ៥. Delete User — ADMIN only
    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete user by ID")
    public ResponseEntity<Map<String, String>> deleteUser(
            @PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok(
                Map.of("message", "User deleted successfully with ID: " + userId));
    }

    // ៦. Promote to ADMIN — ADMIN only
    @PutMapping("/users/{userId}/make-admin")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Promote user to ADMIN role")
    public ResponseEntity<Map<String, String>> makeAdmin(
            @PathVariable Long userId) {
        userService.changeUserRole(userId, "ADMIN");
        return ResponseEntity.ok(
                Map.of("message", "User promoted to ADMIN successfully"));
    }

    // ៧. Demote to USER — ADMIN only
    @PutMapping("/users/{userId}/make-user")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Demote user to USER role")
    public ResponseEntity<Map<String, String>> makeUser(
            @PathVariable Long userId) {
        userService.changeUserRole(userId, "USER");
        return ResponseEntity.ok(
                Map.of("message", "User demoted to USER successfully"));
    }
}