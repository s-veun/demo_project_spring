package com.example.demo_project_spring_boot.service.impl;

import com.example.demo_project_spring_boot.dto.AddressDto;
import com.example.demo_project_spring_boot.dto.ChangePasswordRequest;
import com.example.demo_project_spring_boot.dto.UserProfileResponse;
import com.example.demo_project_spring_boot.exception.DuplicateResourceException;
import com.example.demo_project_spring_boot.Enum.Role;
import com.example.demo_project_spring_boot.exception.ResourceNotFoundException;
import com.example.demo_project_spring_boot.model.Address;
import com.example.demo_project_spring_boot.model.User;
import com.example.demo_project_spring_boot.repository.UserRepository;
import com.example.demo_project_spring_boot.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ✅ Register USER — Force Role.USER ជានិច្ច
    @Override
    public User registerUser(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new DuplicateResourceException(
                    "ឈ្មោះអ្នកប្រើប្រាស់ '" + user.getUsername() + "' មានក្នុងប្រព័ន្ធរួចហើយ");
        }
        // ✅ Force USER role — ignore role ពី request
        user.setRole(Role.USER);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setIsEnabled(true);
        return userRepository.save(user);
    }

    // ✅ Register ADMIN — Force Role.ADMIN
    @Override
    public User registerAdmin(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new DuplicateResourceException(
                    "Username '" + user.getUsername() + "' already exists");
        }
        // ✅ Force ADMIN role — ignore role ពី request
        user.setRole(Role.ADMIN);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setIsEnabled(true);
        return userRepository.save(user);
    }

    @Override
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        username + " Not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("Old password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    // ✅ Get all USERS only (មិនមែន ADMIN)
    @Override
    public List<UserProfileResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToProfileResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "រកមិនឃើញអ្នកប្រើប្រាស់ ID: " + userId));

        boolean currentStatus = user.getIsEnabled() != null
                ? user.getIsEnabled() : true;
        user.setIsEnabled(!currentStatus);
        userRepository.save(user);
    }

    @Override
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException(
                    "មិនអាចលុបបានទេ ព្រោះរកមិនឃើញ User ID: " + userId);
        }
        userRepository.deleteById(userId);
    }

    @Override
    public UserProfileResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "រកមិនឃើញអ្នកប្រើប្រាស់ ID: " + userId));
        return mapToProfileResponse(user);
    }

    @Override
    public UserProfileResponse getMyProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "រកមិនឃើញអ្នកប្រើប្រាស់: " + username));
        return mapToProfileResponse(user);
    }

    @Override
    public void changeUserRole(Long userId, String role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User : " + userId + " Not Found"));
        try {
            user.setRole(Role.valueOf(role.toUpperCase()));
            userRepository.save(user);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(
                    "Invalid role: " + role + ". Must be USER or ADMIN");
        }
    }

    // ✅ Helper — convert User Entity → UserProfileResponse DTO
    private UserProfileResponse mapToProfileResponse(User user) {
        List<AddressDto> addressDtos = user.getAddresses() != null
                ? user.getAddresses().stream()
                  .map(this::mapAddressToDto)
                  .collect(Collectors.toList())
                : List.of();

        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .addresses(addressDtos)
                .build();
    }

    private AddressDto mapAddressToDto(Address addr) {
        return AddressDto.builder()
                .addressId(addr.getAddressId())
                .fullName(addr.getFullName())
                .phoneNumber(addr.getPhoneNumber())
                .city(addr.getCity())
                .district(addr.getDistrict())
                .detailsAddress(addr.getDetailsAddress())
                .userId(addr.getUser().getId())
                .build();
    }
}