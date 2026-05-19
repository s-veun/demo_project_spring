package com.example.demo_project_spring_boot.service;

import com.example.demo_project_spring_boot.dto.BannerRequestDto;
import com.example.demo_project_spring_boot.dto.BannerResponseDto;
import com.example.demo_project_spring_boot.model.Banner;
import com.example.demo_project_spring_boot.repository.BannerRepository;
import com.example.demo_project_spring_boot.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BannerService {

    private final BannerRepository bannerRepository;

    @Value("${banner.upload.dir:uploads/banners}")
    private String uploadDir;

    @Value("${app.api.url:https://api.example.com}")
    private String apiUrl;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(
            Arrays.asList("jpg", "jpeg", "png", "webp")
    );

    /**
     * Create a new banner with optional image upload
     */
    public BannerResponseDto createBanner(BannerRequestDto requestDto) throws IOException {
        log.info("Creating banner: {}", requestDto.getTitle());

        String imageUrl = requestDto.getImageUrl();

        // Handle image file upload if provided
        if (requestDto.getImageFile() != null && !requestDto.getImageFile().isEmpty()) {
            imageUrl = uploadBannerImage(requestDto.getImageFile());
        }

        // If no image provided and no URL set, throw error
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("Banner image is required");
        }

        Banner banner = Banner.builder()
                .title(requestDto.getTitle())
                .subtitle(requestDto.getSubtitle())
                .description(requestDto.getDescription())
                .buttonText(requestDto.getButtonText())
                .buttonLink(requestDto.getButtonLink())
                .imageUrl(imageUrl)
                .backgroundColor(requestDto.getBackgroundColor())
                .textColor(requestDto.getTextColor())
                .badgeText(requestDto.getBadgeText())
                .positionOrder(requestDto.getPositionOrder())
                .isActive(requestDto.getIsActive() != null && requestDto.getIsActive())
                .build();

        Banner savedBanner = bannerRepository.save(banner);
        log.info("Banner created successfully with ID: {}", savedBanner.getBannerId());

        return mapToResponseDto(savedBanner);
    }

    /**
     * Update an existing banner
     */
    public BannerResponseDto updateBanner(Long bannerId, BannerRequestDto requestDto) throws IOException {
        log.info("Updating banner with ID: {}", bannerId);

        Banner banner = bannerRepository.findById(bannerId)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found with ID: " + bannerId));

        // Handle image update if new file is provided
        if (requestDto.getImageFile() != null && !requestDto.getImageFile().isEmpty()) {
            String newImageUrl = uploadBannerImage(requestDto.getImageFile());
            banner.setImageUrl(newImageUrl);
        } else if (requestDto.getImageUrl() != null && !requestDto.getImageUrl().isBlank()) {
            // Use provided image URL if file is not uploaded
            banner.setImageUrl(requestDto.getImageUrl());
        }

        banner.setTitle(requestDto.getTitle());
        banner.setSubtitle(requestDto.getSubtitle());
        banner.setDescription(requestDto.getDescription());
        banner.setButtonText(requestDto.getButtonText());
        banner.setButtonLink(requestDto.getButtonLink());
        banner.setBackgroundColor(requestDto.getBackgroundColor());
        banner.setTextColor(requestDto.getTextColor());
        banner.setBadgeText(requestDto.getBadgeText());
        banner.setPositionOrder(requestDto.getPositionOrder());
        banner.setIsActive(requestDto.getIsActive() != null && requestDto.getIsActive());

        Banner updatedBanner = bannerRepository.save(banner);
        log.info("Banner updated successfully with ID: {}", bannerId);

        return mapToResponseDto(updatedBanner);
    }

    /**
     * Get all active banners sorted by position order
     */
    @Transactional(readOnly = true)
    public List<BannerResponseDto> getAllActiveBanners() {
        log.info("Fetching all active banners");
        return bannerRepository.findAllActiveBannersSortedByPosition()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all banners (including inactive) - for admin
     */
    @Transactional(readOnly = true)
    public List<BannerResponseDto> getAllBanners() {
        log.info("Fetching all banners (including inactive)");
        return bannerRepository.findAllBannersSortedByPosition()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Get banner by ID
     */
    @Transactional(readOnly = true)
    public BannerResponseDto getBannerById(Long bannerId) {
        log.info("Fetching banner with ID: {}", bannerId);
        Banner banner = bannerRepository.findById(bannerId)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found with ID: " + bannerId));
        return mapToResponseDto(banner);
    }

    /**
     * Delete banner by ID
     */
    public void deleteBanner(Long bannerId) {
        log.info("Deleting banner with ID: {}", bannerId);
        Banner banner = bannerRepository.findById(bannerId)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found with ID: " + bannerId));

        bannerRepository.deleteById(bannerId);
        log.info("Banner deleted successfully with ID: {}", bannerId);
    }

    /**
     * Toggle banner active/inactive status
     */
    public BannerResponseDto toggleBannerStatus(Long bannerId) {
        log.info("Toggling banner status for ID: {}", bannerId);

        Banner banner = bannerRepository.findById(bannerId)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found with ID: " + bannerId));

        banner.setIsActive(!banner.getIsActive());
        Banner updatedBanner = bannerRepository.save(banner);

        log.info("Banner status toggled successfully. New status: {}", updatedBanner.getIsActive());
        return mapToResponseDto(updatedBanner);
    }

    /**
     * Update banner position order
     */
    public void updateBannerPositions(Map<Long, Integer> bannerPositions) {
        log.info("Updating banner positions");

        bannerPositions.forEach((bannerId, position) -> {
            Banner banner = bannerRepository.findById(bannerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Banner not found with ID: " + bannerId));
            banner.setPositionOrder(position);
            bannerRepository.save(banner);
        });

        log.info("Banner positions updated successfully");
    }

    /**
     * Upload banner image and return accessible URL
     */
    private String uploadBannerImage(MultipartFile file) throws IOException {
        log.info("Uploading banner image: {}", file.getOriginalFilename());

        // Validate file
        validateImageFile(file);

        // Generate unique filename
        String filename = generateUniqueFileName(file.getOriginalFilename());

        // Ensure upload directory exists
        Path uploadPath = Paths.get(uploadDir);
        Files.createDirectories(uploadPath);

        // Save file
        Path filePath = uploadPath.resolve(filename);
        Files.write(filePath, file.getBytes());

        // Generate accessible URL
        String imageUrl = apiUrl + "/uploads/banners/" + filename;
        log.info("Banner image uploaded successfully: {}", imageUrl);

        return imageUrl;
    }

    /**
     * Validate image file
     */
    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Image file cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 10MB");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.contains(".")) {
            throw new IllegalArgumentException("Invalid file format");
        }

        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Only jpg, jpeg, png, and webp formats are allowed");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File must be a valid image");
        }
    }

    /**
     * Generate unique filename for uploaded image
     */
    private String generateUniqueFileName(String originalFilename) {
        if (originalFilename == null) {
            originalFilename = "banner.jpg";
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString().substring(0, 8);

        return "banner_" + timestamp + "_" + uuid + extension.toLowerCase();
    }

    /**
     * Map Banner entity to BannerResponseDto
     */
    private BannerResponseDto mapToResponseDto(Banner banner) {
        return BannerResponseDto.builder()
                .bannerId(banner.getBannerId())
                .title(banner.getTitle())
                .subtitle(banner.getSubtitle())
                .description(banner.getDescription())
                .buttonText(banner.getButtonText())
                .buttonLink(banner.getButtonLink())
                .imageUrl(banner.getImageUrl())
                .backgroundColor(banner.getBackgroundColor())
                .textColor(banner.getTextColor())
                .badgeText(banner.getBadgeText())
                .positionOrder(banner.getPositionOrder())
                .isActive(banner.getIsActive())
                .createdAt(banner.getCreatedAt())
                .updatedAt(banner.getUpdatedAt())
                .build();
    }
}


