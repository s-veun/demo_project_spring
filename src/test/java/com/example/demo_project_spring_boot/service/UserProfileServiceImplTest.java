package com.example.demo_project_spring_boot.service;

import com.example.demo_project_spring_boot.Enum.AuthProvider;
import com.example.demo_project_spring_boot.Enum.Role;
import com.example.demo_project_spring_boot.config.JwtService;
import com.example.demo_project_spring_boot.config.ProfileImageProperties;
import com.example.demo_project_spring_boot.dto.UploadImageResponse;
import com.example.demo_project_spring_boot.model.User;
import com.example.demo_project_spring_boot.repository.AddressRepository;
import com.example.demo_project_spring_boot.repository.OrderRepository;
import com.example.demo_project_spring_boot.repository.ReviewRepository;
import com.example.demo_project_spring_boot.repository.UserRepository;
import com.example.demo_project_spring_boot.repository.WishlistRepository;
import com.example.demo_project_spring_boot.security.ProfileUploadRateLimiter;
import com.example.demo_project_spring_boot.service.impl.UserProfileServiceImpl;
import com.example.demo_project_spring_boot.utils.PasswordPolicyValidator;
import com.example.demo_project_spring_boot.utils.ProfileImageValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ProfileUploadRateLimiter profileUploadRateLimiter;

    @Mock
    private JwtService jwtService;

    private ProfileImageValidator profileImageValidator;
    private ProfileImageProperties profileImageProperties;
    private PasswordPolicyValidator passwordPolicyValidator;

    @InjectMocks
    private UserProfileServiceImpl userProfileService;

    private User user;

    @BeforeEach
    void setUp() {
        profileImageProperties = new ProfileImageProperties();
        profileImageProperties.setDefaultUrl("/uploads/profile-images/default-avatar.png");
        profileImageProperties.setMaxFileSizeBytes(1024 * 1024);
        profileImageValidator = new ProfileImageValidator(profileImageProperties);
        passwordPolicyValidator = new PasswordPolicyValidator();

        userProfileService = new UserProfileServiceImpl(
                userRepository,
                addressRepository,
                orderRepository,
                wishlistRepository,
                reviewRepository,
                fileStorageService,
                profileImageValidator,
                passwordPolicyValidator,
                passwordEncoder,
                profileUploadRateLimiter,
                profileImageProperties,
                jwtService
        );

        lenient().when(orderRepository.findByUser_IdOrderByOrderDateDesc(1L)).thenReturn(java.util.List.of());
        lenient().when(wishlistRepository.countByUserId(1L)).thenReturn(0L);
        lenient().when(reviewRepository.countByUser_Id(1L)).thenReturn(0L);
        lenient().when(addressRepository.findByUser_Id(1L)).thenReturn(java.util.List.of());

        user = User.builder()
                .id(1L)
                .username("alice")
                .email("alice@example.com")
                .role(Role.USER)
                .provider(AuthProvider.LOCAL)
                .isEnabled(true)
                .build();
    }

    @Test
    void uploadProfileImageReplacesOldFileAndPersistsMetadata() {
        user.setProfileImageName("old.png");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(fileStorageService.storeProfileImage(any(), any(), any())).thenReturn(new StoredImage("new.png", "/uploads/profile-images/new.png"));

        byte[] png = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", png);

        UploadImageResponse response = userProfileService.uploadProfileImage("alice", file);

        verify(profileUploadRateLimiter).validateRequest("alice");
        verify(fileStorageService).deleteProfileImage("old.png");
        verify(userRepository).save(user);
        assertEquals("/uploads/profile-images/new.png", response.getImageUrl());
    }

    @Test
    void deleteProfileImageClearsStoredValues() {
        user.setProfileImageName("avatar.png");
        user.setProfileImageUrl("/uploads/profile-images/avatar.png");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var profile = userProfileService.deleteProfileImage("alice");

        verify(fileStorageService).deleteProfileImage("avatar.png");
        assertNull(user.getProfileImageName());
        assertNull(user.getProfileImageUrl());
        assertEquals("/uploads/profile-images/default-avatar.png", profile.getProfileImageUrl());
    }
}
