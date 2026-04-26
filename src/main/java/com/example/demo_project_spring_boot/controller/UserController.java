package com.example.demo_project_spring_boot.controller;

import com.example.demo_project_spring_boot.config.JwtService;
import com.example.demo_project_spring_boot.dto.ChangePasswordRequest;
import com.example.demo_project_spring_boot.dto.LoginRequest;
import com.example.demo_project_spring_boot.dto.UserProfileResponse;
import com.example.demo_project_spring_boot.dto.RegisterRequest;
import com.example.demo_project_spring_boot.model.User;
import com.example.demo_project_spring_boot.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "User", description = "User Authentication & Profile APIs")
public class UserController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;
    private final UserDetailsService userDetailsService;

    // ✅ ប្រើ RegisterRequest DTO ជំនួស User entity
    @Operation(summary = "Register new user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request")
    })
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest request) {
        // ✅ convert RegisterRequest → User
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());

        User registeredUser = userService.registerUser(user);
        registeredUser.setPassword(null);
        return new ResponseEntity<>(registeredUser, HttpStatus.CREATED);
    }

    // ✅ ប្រើ LoginRequest DTO ជំនួស User entity
    @Operation(summary = "Login and get JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
            String jwtToken = jwtService.generateToken(userDetails);

            Map<String, Object> response = new HashMap<>();
            response.put("token", jwtToken);
            response.put("username", userDetails.getUsername());
            response.put("roles", userDetails.getAuthorities());
            response.put("message", "User logged in successfully");

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            return new ResponseEntity<>("ឈ្មោះអ្នកប្រើប្រាស់ ឬលេខសម្ងាត់មិនត្រឹមត្រូវ", HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            return new ResponseEntity<>("ការ Login បរាជ័យ: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "Get current user profile", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(Authentication authentication) {
        UserProfileResponse response = userService.getMyProfile(authentication.getName());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get current user (alias)", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMe(Authentication authentication) {
        UserProfileResponse response = userService.getMyProfile(authentication.getName());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Change password", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password changed successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping("/profile/change-password")
    public ResponseEntity<?> changePassword(
            Authentication authentication,
            @RequestBody ChangePasswordRequest request) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return new ResponseEntity<>("Please Login First!", HttpStatus.UNAUTHORIZED);
        }

        try {
            userService.changePassword(authentication.getName(), request);
            return ResponseEntity.ok("Password changed successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}