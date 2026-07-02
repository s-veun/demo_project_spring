package com.example.demo_project_spring_boot.service.impl;

import com.example.demo_project_spring_boot.config.JwtService;
import com.example.demo_project_spring_boot.config.ProfileImageProperties;
import com.example.demo_project_spring_boot.dto.AddressDto;
import com.example.demo_project_spring_boot.dto.UpdateProfileWithTokenResponse;
import com.example.demo_project_spring_boot.dto.UpdateProfileWithTokenResponse.UpdatedSecurityField;
import com.example.demo_project_spring_boot.dto.UploadImageResponse;
import com.example.demo_project_spring_boot.dto.UserProfileResponse;
import com.example.demo_project_spring_boot.dto.request.ChangeUserPasswordRequest;
import com.example.demo_project_spring_boot.dto.request.NotificationSettingsRequest;
import com.example.demo_project_spring_boot.dto.request.UpdateUserProfileRequest;
import com.example.demo_project_spring_boot.dto.request.UserAddressRequest;
import com.example.demo_project_spring_boot.dto.response.UserAddressResponse;
import com.example.demo_project_spring_boot.exception.BadRequestException;
import com.example.demo_project_spring_boot.exception.DuplicateResourceException;
import com.example.demo_project_spring_boot.exception.ResourceNotFoundException;
import com.example.demo_project_spring_boot.model.Address;
import com.example.demo_project_spring_boot.model.User;
import com.example.demo_project_spring_boot.repository.AddressRepository;
import com.example.demo_project_spring_boot.repository.OrderRepository;
import com.example.demo_project_spring_boot.repository.ReviewRepository;
import com.example.demo_project_spring_boot.repository.UserRepository;
import com.example.demo_project_spring_boot.repository.WishlistRepository;
import com.example.demo_project_spring_boot.security.ProfileUploadRateLimiter;
import com.example.demo_project_spring_boot.service.FileStorageService;
import com.example.demo_project_spring_boot.service.StoredImage;
import com.example.demo_project_spring_boot.service.UserProfileService;
import com.example.demo_project_spring_boot.utils.PasswordPolicyValidator;
import com.example.demo_project_spring_boot.utils.ProfileImageValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserProfileServiceImpl implements UserProfileService {

    private static final Pattern URL_PATTERN = Pattern.compile("^(https?://).+");

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final WishlistRepository wishlistRepository;
    private final ReviewRepository reviewRepository;
    private final FileStorageService fileStorageService;
    private final ProfileImageValidator profileImageValidator;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final PasswordEncoder passwordEncoder;
    private final ProfileUploadRateLimiter profileUploadRateLimiter;
    private final ProfileImageProperties profileImageProperties;
    private final JwtService jwtService;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(String principalName) {
        User user = findUserByPrincipal(principalName);
        return mapToProfileResponse(user);
    }

    @Override
    public UserProfileResponse updateMyProfile(String principalName, UpdateUserProfileRequest request) {
        User user = findUserByPrincipal(principalName);

        // Detect security field changes before updating
        String oldUsername = user.getUsername();
        String oldEmail = user.getEmail();

        updateProfileFields(user, request);

        User saved = userRepository.save(user);
        log.info("Profile updated. userId={}, oldUsername={}, newUsername={}, oldEmail={}, newEmail={}",
                saved.getId(), oldUsername, saved.getUsername(), oldEmail, saved.getEmail());
        
        return mapToProfileResponse(saved);
    }

    /**
     * Update profile and handle token regeneration if username/email changed
     * Returns both profile data and new tokens if regeneration occurred
     */
    public UpdateProfileWithTokenResponse updateMyProfileWithTokens(
            String principalName,
            UpdateUserProfileRequest request) {
        
        User user = findUserByPrincipal(principalName);

        String oldUsername = user.getUsername();
        String oldEmail = user.getEmail();

        updateProfileFields(user, request);

        User saved = userRepository.save(user);
        
        // Detect which security fields changed
        UpdatedSecurityField securityFieldUpdated = detectSecurityFieldChanges(oldUsername, oldEmail, saved);
        
        if (securityFieldUpdated != UpdatedSecurityField.NONE) {
            log.info("[SECURITY] Security field changed for user: {} | field: {} | oldUsername: {} → newUsername: {} | oldEmail: {} → newEmail: {}",
                    saved.getId(), securityFieldUpdated, oldUsername, saved.getUsername(), oldEmail, saved.getEmail());
            
            // Generate new tokens with updated username/email
            String newAccessToken = jwtService.generateAccessToken(
                    saved.getId(),
                    saved.getUsername(),
                    saved.getEmail(),
                    saved.getRole().name()
            );
            
            String newRefreshToken = jwtService.generateRefreshToken(
                    saved.getUsername(),
                    saved.getId()
            );
            
            // Store new tokens in database
            saved.setAccessToken(newAccessToken);
            saved.setRefreshToken(newRefreshToken);
            userRepository.save(saved);
            
            log.info("[SECURITY] New JWT tokens generated for user: {} | username: {} | timestamp: {}",
                    saved.getId(), saved.getUsername(), System.currentTimeMillis());
            
            return UpdateProfileWithTokenResponse.builder()
                    .success(true)
                    .message("Profile updated successfully. New authentication tokens generated due to username/email change.")
                    .profile(mapToProfileResponse(saved))
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtService.getAccessTokenExpirationSeconds())
                    .tokensRegenerated(true)
                    .securityFieldUpdated(securityFieldUpdated)
                    .build();
        } else {
            // No security fields changed, return profile only
            return UpdateProfileWithTokenResponse.builder()
                    .success(true)
                    .message("Profile updated successfully")
                    .profile(mapToProfileResponse(saved))
                    .tokensRegenerated(false)
                    .securityFieldUpdated(UpdatedSecurityField.NONE)
                    .build();
        }
    }

    /**
     * Detect which security fields (username or email) were changed
     */
    private UpdatedSecurityField detectSecurityFieldChanges(
            String oldUsername,
            String oldEmail,
            User updatedUser) {
        
        boolean usernameChanged = !Objects.equals(oldUsername, updatedUser.getUsername());
        boolean emailChanged = !Objects.equals(oldEmail, updatedUser.getEmail());
        
        if (usernameChanged && emailChanged) {
            return UpdatedSecurityField.BOTH;
        } else if (usernameChanged) {
            return UpdatedSecurityField.USERNAME;
        } else if (emailChanged) {
            return UpdatedSecurityField.EMAIL;
        } else {
            return UpdatedSecurityField.NONE;
        }
    }

    /**
     * Apply requested profile field updates to user entity
     */
    private void updateProfileFields(User user, UpdateUserProfileRequest request) {
        if (StringUtils.hasText(request.getUsername())) {
            String normalized = normalize(request.getUsername());
            userRepository.findByUsername(normalized)
                    .filter(found -> !Objects.equals(found.getId(), user.getId()))
                    .ifPresent(found -> {
                        throw new DuplicateResourceException("Username already taken");
                    });
            user.setUsername(normalized);
        }

        if (StringUtils.hasText(request.getFullName())) {
            user.setFullName(normalize(request.getFullName()));
        }

        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(normalize(request.getPhoneNumber()));
        }

        if (StringUtils.hasText(request.getGender())) {
            user.setGender(request.getGender().trim().toUpperCase());
        }

        user.setDateOfBirth(request.getDateOfBirth() != null ? request.getDateOfBirth() : user.getDateOfBirth());

        if (request.getBio() != null) {
            user.setBio(normalize(request.getBio()));
        }
        if (request.getCountry() != null) {
            user.setCountry(normalize(request.getCountry()));
        }
        if (request.getCity() != null) {
            user.setCity(normalize(request.getCity()));
        }

        if (request.getFacebookUrl() != null) {
            user.setFacebookUrl(normalizeOptionalUrl(request.getFacebookUrl(), "Facebook URL"));
        }
        if (request.getInstagramUrl() != null) {
            user.setInstagramUrl(normalizeOptionalUrl(request.getInstagramUrl(), "Instagram URL"));
        }
        if (request.getLinkedInUrl() != null) {
            user.setLinkedInUrl(normalizeOptionalUrl(request.getLinkedInUrl(), "LinkedIn URL"));
        }
        if (request.getTelegramHandle() != null) {
            user.setTelegramHandle(normalize(request.getTelegramHandle()));
        }
    }

    @Override
    public void changePassword(String principalName, ChangeUserPasswordRequest request) {
        User user = findUserByPrincipal(principalName);
        if (user.getPassword() == null) {
            throw new BadRequestException("Password login is not enabled for this account");
        }

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("Old password is incorrect");
        }

        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new BadRequestException("New password must be different from old password");
        }

        passwordPolicyValidator.validate(request.getNewPassword());
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for userId={}", user.getId());
    }

    @Override
    public UploadImageResponse uploadProfileImage(String principalName, MultipartFile file) {
        User user = findUserByPrincipal(principalName);
        profileUploadRateLimiter.validateRequest(user.getUsername());
        String extension = profileImageValidator.validateAndExtractExtension(file);

        String oldImageName = user.getProfileImageName();
        if (StringUtils.hasText(oldImageName)) {
            fileStorageService.deleteProfileImage(oldImageName);
        }

        StoredImage storedImage = fileStorageService.storeProfileImage(file, user.getUsername(), extension);
        user.setProfileImageName(storedImage.imageName());
        user.setProfileImageUrl(storedImage.imageUrl());
        userRepository.save(user);

        log.info("Profile image updated for user={}", user.getUsername());

        return UploadImageResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .imageUrl(storedImage.imageUrl())
                .uploadTimestamp(OffsetDateTime.now())
                .build();
    }

    @Override
    public UserProfileResponse deleteProfileImage(String principalName) {
        User user = findUserByPrincipal(principalName);

        String oldImageName = user.getProfileImageName();
        if (StringUtils.hasText(oldImageName)) {
            fileStorageService.deleteProfileImage(oldImageName);
        }

        user.setProfileImageName(null);
        user.setProfileImageUrl(null);
        User saved = userRepository.save(user);

        log.info("Profile image deleted for user={}", user.getUsername());

        return mapToProfileResponse(saved);
    }

    @Override
    public List<UserAddressResponse> getMyAddresses(String principalName) {
        User user = findUserByPrincipal(principalName);
        return addressRepository.findByUser_Id(user.getId()).stream()
                .map(this::mapAddress)
                .collect(Collectors.toList());
    }

    @Override
    public UserAddressResponse addAddress(String principalName, UserAddressRequest request) {
        User user = findUserByPrincipal(principalName);

        if (request.getIsDefault() == Boolean.TRUE) {
            clearDefaultAddress(user.getId());
        }

        Address address = new Address();
        address.setUser(user);
        address.setFullName(normalize(request.getFullName()));
        address.setPhoneNumber(normalize(request.getPhoneNumber()));
        address.setCountry(normalize(request.getCountry()));
        address.setCity(normalize(request.getCity()));
        address.setState(normalize(request.getState()));
        address.setDistrict(normalize(request.getDistrict()));
        address.setPostalCode(normalize(request.getPostalCode()));
        address.setAddressLine1(normalize(request.getAddressLine1()));
        address.setAddressLine2(normalize(request.getAddressLine2()));
        address.setDetailsAddress(normalize(request.getDetailsAddress()));
        address.setIsDefault(request.getIsDefault() == Boolean.TRUE);

        Address saved = addressRepository.save(address);
        return mapAddress(saved);
    }

    @Override
    public UserAddressResponse updateAddress(String principalName, Long addressId, UserAddressRequest request) {
        User user = findUserByPrincipal(principalName);
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        if (!Objects.equals(address.getUser().getId(), user.getId())) {
            throw new BadRequestException("Cannot modify address of another user");
        }

        if (request.getIsDefault() == Boolean.TRUE) {
            clearDefaultAddress(user.getId());
        }

        applyAddressRequest(address, request);
        Address saved = addressRepository.save(address);
        return mapAddress(saved);
    }

    @Override
    public void deleteAddress(String principalName, Long addressId) {
        User user = findUserByPrincipal(principalName);
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        if (!Objects.equals(address.getUser().getId(), user.getId())) {
            throw new BadRequestException("Cannot delete address of another user");
        }

        addressRepository.delete(address);
    }

    @Override
    public UserProfileResponse updateNotificationSettings(String principalName, NotificationSettingsRequest request) {
        User user = findUserByPrincipal(principalName);

        if (request.getEmailNotificationsEnabled() != null) {
            user.setEmailNotificationsEnabled(request.getEmailNotificationsEnabled());
        }
        if (request.getSmsNotificationsEnabled() != null) {
            user.setSmsNotificationsEnabled(request.getSmsNotificationsEnabled());
        }
        if (request.getMarketingNotificationsEnabled() != null) {
            user.setMarketingNotificationsEnabled(request.getMarketingNotificationsEnabled());
        }
        if (request.getSecurityAlertsEnabled() != null) {
            user.setSecurityAlertsEnabled(request.getSecurityAlertsEnabled());
        }

        User saved = userRepository.save(user);
        log.info("Notification settings updated for userId={}", saved.getId());
        return mapToProfileResponse(saved);
    }

    @Transactional(readOnly = true)
    protected User findUserByPrincipal(String principalName) {
        return userRepository.findByUsername(principalName)
                .or(() -> userRepository.findByEmail(principalName))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + principalName));
    }

    @Transactional(readOnly = true)
    private UserProfileResponse mapToProfileResponse(User user) {
        List<AddressDto> addressDtos = user.getAddresses() != null
                ? user.getAddresses().stream()
                  .filter(addr -> addr != null)
                  .map(this::mapAddressToDto)
                  .collect(Collectors.toList())
                : List.of();

        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .bio(user.getBio())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth())
                .country(user.getCountry())
                .city(user.getCity())
                .profileImageUrl(user.getProfileImageUrl())
                .emailVerified(user.getEmailVerified())
                .phoneVerified(user.getPhoneVerified())
                .emailVerifiedAt(user.getEmailVerifiedAt())
                .phoneVerifiedAt(user.getPhoneVerifiedAt())
                .facebookUrl(user.getFacebookUrl())
                .telegramHandle(user.getTelegramHandle())
                .instagramUrl(user.getInstagramUrl())
                .linkedInUrl(user.getLinkedInUrl())
                .emailNotificationsEnabled(user.getEmailNotificationsEnabled())
                .smsNotificationsEnabled(user.getSmsNotificationsEnabled())
                .marketingNotificationsEnabled(user.getMarketingNotificationsEnabled())
                .securityAlertsEnabled(user.getSecurityAlertsEnabled())
                .role(user.getRole())
                .isEnabled(user.getIsEnabled())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .totalOrders((long) orderRepository.findByUser_IdOrderByOrderDateDesc(user.getId()).size())
                .wishlistCount(wishlistRepository.countByUserId(user.getId()))
                .reviewCount(reviewRepository.countByUser_Id(user.getId()))
                .profileCompletion(calculateProfileCompletion(user))
                .addresses(addressDtos)
                .build();
    }

    private UserAddressResponse mapAddress(Address address) {
        return UserAddressResponse.builder()
                .id(address.getAddressId())
                .fullName(address.getFullName())
                .phoneNumber(address.getPhoneNumber())
                .country(address.getCountry())
                .city(address.getCity())
                .state(address.getState())
                .district(address.getDistrict())
                .postalCode(address.getPostalCode())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .detailsAddress(address.getDetailsAddress())
                .isDefault(address.getIsDefault())
                .build();
    }

    private void applyAddressRequest(Address address, UserAddressRequest request) {
        address.setFullName(normalize(request.getFullName()));
        address.setPhoneNumber(normalize(request.getPhoneNumber()));
        address.setCountry(normalize(request.getCountry()));
        address.setCity(normalize(request.getCity()));
        address.setState(normalize(request.getState()));
        address.setDistrict(normalize(request.getDistrict()));
        address.setPostalCode(normalize(request.getPostalCode()));
        address.setAddressLine1(normalize(request.getAddressLine1()));
        address.setAddressLine2(normalize(request.getAddressLine2()));
        address.setDetailsAddress(normalize(request.getDetailsAddress()));
        if (request.getIsDefault() != null) {
            address.setIsDefault(request.getIsDefault());
        }
    }

    private void clearDefaultAddress(Long userId) {
        addressRepository.findByUser_IdAndIsDefaultTrue(userId).ifPresent(existing -> {
            existing.setIsDefault(false);
            addressRepository.save(existing);
        });
    }

    private int calculateProfileCompletion(User user) {
        int filled = 0;
        int total = 8;

        if (StringUtils.hasText(user.getProfileImageUrl())) filled++;
        if (StringUtils.hasText(user.getPhoneNumber())) filled++;
        if (StringUtils.hasText(user.getBio())) filled++;
        if (StringUtils.hasText(user.getFullName())) filled++;
        if (Boolean.TRUE.equals(user.getEmailVerified())) filled++;
        if (Boolean.TRUE.equals(user.getPhoneVerified())) filled++;
        if (user.getDateOfBirth() != null) filled++;
        if (!addressRepository.findByUser_Id(user.getId()).isEmpty()) filled++;

        return (filled * 100) / total;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeOptionalUrl(String value, String fieldName) {
        String normalized = normalize(value);
        if (!StringUtils.hasText(normalized)) {
            return normalized;
        }
        if (!URL_PATTERN.matcher(normalized).matches()) {
            throw new BadRequestException(fieldName + " must start with http:// or https://");
        }
        return normalized;
    }

    private AddressDto mapAddressToDto(Address address) {
        return AddressDto.builder()
                .addressId(address.getAddressId())
                .fullName(address.getFullName())
                .phoneNumber(address.getPhoneNumber())
                .city(address.getCity())
                .district(address.getDistrict())
                .detailsAddress(address.getDetailsAddress())
                .userId(address.getUser() != null ? address.getUser().getId() : null)
                .build();
    }
}
