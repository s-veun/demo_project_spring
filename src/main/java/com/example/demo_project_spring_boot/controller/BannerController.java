package com.example.demo_project_spring_boot.controller;

import com.example.demo_project_spring_boot.dto.BannerRequestDto;
import com.example.demo_project_spring_boot.dto.BannerResponseDto;
import com.example.demo_project_spring_boot.service.BannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/banners")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Banners", description = "Banner management APIs for homepage and admin dashboard")
public class BannerController {

  private final BannerService bannerService;

  // ======================================
  // PUBLIC ENDPOINTS (No Authentication)
  // ======================================

  /**
   * Get all active banners for homepage display
   */
  @GetMapping
    @Operation(summary = "Get all active banners",
            description = "Fetch all active banners sorted by position order for homepage display")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of active banners retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<BannerResponseDto>> getActiveBanners() {
        log.info("Fetching all active banners");
        try {
            List<BannerResponseDto> banners = bannerService.getAllActiveBanners();
            return ResponseEntity.ok(banners);
        } catch (Exception e) {
            log.error("Error fetching active banners", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get banner details by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get banner by ID",
            description = "Retrieve detailed information about a specific banner")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Banner retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Banner not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getBannerById(
            @PathVariable
            @Parameter(description = "Banner ID", required = true, example = "1")
            Long id) {
        try {
            BannerResponseDto banner = bannerService.getBannerById(id);
            return ResponseEntity.ok(banner);
        } catch (RuntimeException e) {
            log.error("Error fetching banner with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
    }
  }

  // ======================================
  // ADMIN ENDPOINTS (Requires Authentication)
  // ======================================

  /**
   * Create new banner with image upload
   */
  @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create new banner",
            description = "Create a new banner with optional image upload")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Banner created successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request or validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> createBanner(
            @RequestParam String title,
            @RequestParam(required = false) String subtitle,
            @RequestParam(required = false) String description,
            @RequestParam String buttonText,
            @RequestParam String buttonLink,
            @RequestParam String backgroundColor,
            @RequestParam String textColor,
            @RequestParam(required = false) String badgeText,
            @RequestParam Integer positionOrder,
            @RequestParam(required = false, defaultValue = "true") Boolean isActive,
            @RequestParam(required = false)
            @Parameter(description = "Banner image file (jpg, jpeg, png, webp)", content = @Content(mediaType = "image/jpeg"))
            MultipartFile imageFile,
            @RequestParam(required = false) String imageUrl) {
        try {
            BannerRequestDto requestDto = BannerRequestDto.builder()
                    .title(title)
                    .subtitle(subtitle)
                    .description(description)
                    .buttonText(buttonText)
                    .buttonLink(buttonLink)
                    .backgroundColor(backgroundColor)
                    .textColor(textColor)
                    .badgeText(badgeText)
                    .positionOrder(positionOrder)
                    .isActive(isActive)
                    .imageFile(imageFile)
                    .imageUrl(imageUrl)
                    .build();

            BannerResponseDto savedBanner = bannerService.createBanner(requestDto);
            log.info("Banner created successfully with ID: {}", savedBanner.getBannerId());
            return new ResponseEntity<>(savedBanner, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            log.error("Validation error creating banner", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            log.error("Error uploading file", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "File upload failed: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating banner", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Update banner details and image
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update banner",
            description = "Update banner details and optionally upload new image")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Banner updated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request or validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Banner not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> updateBanner(
            @PathVariable
            @Parameter(description = "Banner ID", required = true)
            Long id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String subtitle,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String buttonText,
            @RequestParam(required = false) String buttonLink,
            @RequestParam(required = false) String backgroundColor,
            @RequestParam(required = false) String textColor,
            @RequestParam(required = false) String badgeText,
            @RequestParam(required = false) Integer positionOrder,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) MultipartFile imageFile,
            @RequestParam(required = false) String imageUrl) {
        try {
            // Fetch current banner to preserve non-updated fields
            BannerResponseDto currentBanner = bannerService.getBannerById(id);

            BannerRequestDto requestDto = BannerRequestDto.builder()
                    .title(title != null ? title : currentBanner.getTitle())
                    .subtitle(subtitle != null ? subtitle : currentBanner.getSubtitle())
                    .description(description != null ? description : currentBanner.getDescription())
                    .buttonText(buttonText != null ? buttonText : currentBanner.getButtonText())
                    .buttonLink(buttonLink != null ? buttonLink : currentBanner.getButtonLink())
                    .backgroundColor(backgroundColor != null ? backgroundColor : currentBanner.getBackgroundColor())
                    .textColor(textColor != null ? textColor : currentBanner.getTextColor())
                    .badgeText(badgeText != null ? badgeText : currentBanner.getBadgeText())
                    .positionOrder(positionOrder != null ? positionOrder : currentBanner.getPositionOrder())
                    .isActive(isActive != null ? isActive : currentBanner.getIsActive())
                    .imageFile(imageFile)
                    .imageUrl(imageUrl)
                    .build();

            BannerResponseDto updatedBanner = bannerService.updateBanner(id, requestDto);
            log.info("Banner updated successfully with ID: {}", id);
            return ResponseEntity.ok(updatedBanner);
        } catch (RuntimeException e) {
            log.error("Error updating banner with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            log.error("Error uploading file", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "File upload failed: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating banner", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Delete banner
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete banner",
            description = "Permanently delete a banner by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Banner deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Banner not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> deleteBanner(
            @PathVariable
            @Parameter(description = "Banner ID", required = true)
            Long id) {
        try {
            bannerService.deleteBanner(id);
            log.info("Banner deleted successfully with ID: {}", id);
            return ResponseEntity.ok(Map.of("message", "Banner deleted successfully"));
        } catch (RuntimeException e) {
            log.error("Error deleting banner with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting banner", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Toggle banner active/inactive status
     */
    @PatchMapping("/status/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Toggle banner status",
            description = "Enable or disable a banner")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Banner status updated successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Banner not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> toggleBannerStatus(
            @PathVariable
            @Parameter(description = "Banner ID", required = true)
            Long id) {
        try {
            BannerResponseDto updatedBanner = bannerService.toggleBannerStatus(id);
            log.info("Banner status toggled successfully for ID: {}", id);
            return ResponseEntity.ok(updatedBanner);
        } catch (RuntimeException e) {
            log.error("Error toggling banner status for ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error toggling banner status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Get all banners (including inactive) - for admin dashboard
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get all banners (Admin only)",
            description = "Retrieve all banners including inactive ones for admin management")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of all banners retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getAllBanners() {
        try {
            List<BannerResponseDto> banners = bannerService.getAllBanners();
            return ResponseEntity.ok(banners);
        } catch (Exception e) {
            log.error("Error fetching all banners", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Update banner positions (reorder)
     */
    @PatchMapping("/positions")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update banner positions",
            description = "Reorder banners by updating their position order values")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Positions updated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> updateBannerPositions(
            @RequestBody
            @Parameter(description = "Map of Banner ID to Position Order", required = true)
            Map<Long, Integer> bannerPositions) {
        try {
            bannerService.updateBannerPositions(bannerPositions);
            log.info("Banner positions updated successfully");
            return ResponseEntity.ok(Map.of("message", "Positions updated successfully"));
        } catch (RuntimeException e) {
            log.error("Error updating banner positions", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating positions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }
}




