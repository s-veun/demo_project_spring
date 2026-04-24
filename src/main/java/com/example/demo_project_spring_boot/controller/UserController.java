package com.example.demo_project_spring_boot.controller;

import com.example.demo_project_spring_boot.config.JwtService;
import com.example.demo_project_spring_boot.dto.ChangePasswordRequest;
import com.example.demo_project_spring_boot.dto.UserProfileResponse;
import com.example.demo_project_spring_boot.model.User;
import com.example.demo_project_spring_boot.service.UserService;
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

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    // 🌟 ហៅ UserService Interface ដែលយើងបានកែ
    private final UserService userService;

    // 🌟 ហៅ UserDetailsService (វានឹងទាញយក CustomUserDetailsService មកប្រើដោយស្វ័យប្រវត្តិ)
    private final UserDetailsService userDetailsService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        User registeredUser = userService.registerUser(user);
        registeredUser.setPassword(null); // លាក់ password ពេលបញ្ជូនទៅវិញ
        return new ResponseEntity<>(registeredUser, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User logonRequest){
        try {
            // ផ្ទៀងផ្ទាត់ជាមួយ Security
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            logonRequest.getUsername(),
                            logonRequest.getPassword()
                    )
            );

            // បង្កើត JWT Token
            UserDetails userDetails = userDetailsService.loadUserByUsername(logonRequest.getUsername());
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

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(Authentication authentication) {
        // authentication.getName() នឹងផ្ដល់ username ដែលដកចេញពី JWT Token
        UserProfileResponse response = userService.getMyProfile(authentication.getName());
        return ResponseEntity.ok(response);
    }


    @PutMapping("/profile/change-password")
    public ResponseEntity<?> changePassword(
            Authentication authentication,
            @RequestBody ChangePasswordRequest request) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return new ResponseEntity<>("Please Login First !", HttpStatus.UNAUTHORIZED);
        }

        try {
            userService.changePassword(authentication.getName(), request);
            return ResponseEntity.ok("Password changed successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}