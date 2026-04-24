package com.example.demo_project_spring_boot.service.impl;

import com.example.demo_project_spring_boot.dto.AddressDto;
import com.example.demo_project_spring_boot.dto.ChangePasswordRequest;
import com.example.demo_project_spring_boot.dto.UserProfileResponse;
import com.example.demo_project_spring_boot.exception.DuplicateResourceException;
import com.example.demo_project_spring_boot.Enum.Role;
import com.example.demo_project_spring_boot.model.User;
import com.example.demo_project_spring_boot.model.Address;
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

    @Override
    public User registerUser(User user) {
        // ១. ពិនិត្យមើលឈ្មោះអ្នកប្រើប្រាស់ជាន់គ្នា
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new DuplicateResourceException("ឈ្មោះអ្នកប្រើប្រាស់ '" + user.getUsername() + "' មានក្នុងប្រព័ន្ធរួចហើយ");
        }

        // ២. ធ្វើការ Encode Password ដើម្បីសុវត្ថិភាព
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // ៣. កំណត់ Role ជា USER ជាលំនាំដើម ប្រសិនបើមិនបានបញ្ជូនមក
        if (user.getRole() == null) {
            user.setRole(Role.USER);
        }

        if (user.getIsEnabled() == null) {
            user.setIsEnabled(true);
        }

        return userRepository.save(user);
    }

    @Override
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException(username + "Not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("Passwords don't match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    public List<UserProfileResponse> getAllUsers() {
        return userRepository.findByRole(Role.USER).stream()
                .map(this::mapToProfileResponse)
                .toList();
    }

    @Override
    public void toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("រកមិនឃើញអ្នកប្រើប្រាស់ ID: " + userId));

        boolean currentStatus = user.getIsEnabled() != null ? user.getIsEnabled() : true;

        // ២. ប្ដូរ Status ថ្មី (ប្រើ setIsEnabled)
        user.setIsEnabled(!currentStatus);

        userRepository.save(user);
    }

    @Override
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("មិនអាចលុបបានទេ ព្រោះរកមិនឃើញ User ID: " + userId);
        }
        userRepository.deleteById(userId);
    }

    /**
     * ប្រើសម្រាប់ Admin ដែលចង់មើល Profile របស់ User ផ្សេងទៀតតាមរយៈ ID
     */
    @Override
    public UserProfileResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("រកមិនឃើញអ្នកប្រើប្រាស់ ID: " + userId));
        return mapToProfileResponse(user);
    }

    /**
     * ប្រើសម្រាប់ម្ចាស់ Profile ខ្លួនឯង (ទាំង User និង Admin) បន្ទាប់ពី Login ជោគជ័យ
     */
    @Override
    public UserProfileResponse getMyProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("រកមិនឃើញអ្នកប្រើប្រាស់: " + username));

        return mapToProfileResponse(user);
    }

    // --- Helper Method សម្រាប់បំប្លែង Entity ទៅជា DTO (ដើម្បីកុំឱ្យសរសេរកូដជាន់គ្នា) ---
    private UserProfileResponse mapToProfileResponse(User user) {
        // បំប្លែងបញ្ជីអាសយដ្ឋាន (Addresses) ទៅជា DTO
        List<AddressDto> addressDtos = user.getAddresses() != null ?
                user.getAddresses().stream()
                        .map(this::mapAddressToDto)
                        .collect(Collectors.toList()) : List.of();

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