package com.example.demo_project_spring_boot.controller;

import com.example.demo_project_spring_boot.config.JwtService;
import com.example.demo_project_spring_boot.dto.*;
import com.example.demo_project_spring_boot.Enum.OrderStatus;
import com.example.demo_project_spring_boot.Enum.Role;
import com.example.demo_project_spring_boot.model.Order;
import com.example.demo_project_spring_boot.model.Product;
import com.example.demo_project_spring_boot.model.ProductImage;
import com.example.demo_project_spring_boot.model.User;
import com.example.demo_project_spring_boot.repository.OrderRepository;
import com.example.demo_project_spring_boot.repository.ProductReposity;
import com.example.demo_project_spring_boot.repository.UserRepository;
import com.example.demo_project_spring_boot.service.AdminProfileService;
import com.example.demo_project_spring_boot.service.OrderService;
import com.example.demo_project_spring_boot.service.ProductService;
import com.example.demo_project_spring_boot.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin management APIs")
public class AdminController {

    private final UserService userService;
    private final AdminProfileService adminProfileService;
    private final OrderService orderService;
    private final ProductService productService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductReposity productReposity;
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

            // Update last login time
            userRepository.findByUsername(request.getUsername()).ifPresent(admin -> {
                admin.setLastLoginAt(java.time.LocalDateTime.now());
                userRepository.save(admin);
            });

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

    // ==========================================
    // ADMIN PROFILE MANAGEMENT
    // ==========================================

    // ៨. Get Admin Profile — ADMIN only
    @GetMapping("/profile")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get current admin profile")
    public ResponseEntity<?> getAdminProfile(Authentication authentication) {
        try {
            AdminProfileResponse profile = adminProfileService.getAdminProfile(authentication.getName());
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ៩. Update Admin Profile — ADMIN only
    @PutMapping("/profile")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update admin profile (partial update)")
    public ResponseEntity<?> updateAdminProfile(
            Authentication authentication,
            @RequestBody UpdateAdminProfileRequest request) {
        try {
            AdminProfileResponse updatedProfile = adminProfileService.updateAdminProfile(
                    authentication.getName(), request);
            return ResponseEntity.ok(Map.of(
                    "message", "Profile updated successfully",
                    "profile", updatedProfile
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ១០. Update Admin Profile Image — ADMIN only
    @PutMapping("/profile/image")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update admin profile image URL")
    public ResponseEntity<?> updateProfileImage(
            Authentication authentication,
            @RequestBody Map<String, String> request) {
        try {
            String imageUrl = request.get("imageUrl");
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Image URL is required"));
            }

            AdminProfileResponse updatedProfile = adminProfileService.updateProfileImage(
                    authentication.getName(), imageUrl.trim());
            return ResponseEntity.ok(Map.of(
                    "message", "Profile image updated successfully",
                    "profileImageUrl", updatedProfile.getProfileImageUrl()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ១១. Get Admin Dashboard Stats — ADMIN only
    @GetMapping("/dashboard/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get admin dashboard statistics")
    public ResponseEntity<?> getDashboardStats(Authentication authentication) {
        try {
            AdminProfileResponse stats = adminProfileService.getAdminDashboardStats(
                    authentication.getName());
            return ResponseEntity.ok(Map.of(
                    "admin", stats,
                    "statistics", Map.of(
                            "totalUsers", stats.getTotalUsersManaged(),
                            "totalOrders", stats.getTotalOrdersManaged(),
                            "totalProducts", stats.getTotalProductsManaged()
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ==========================================
    // ADVANCED USER MANAGEMENT
    // ==========================================

   // ១២. Search & Filter Users — ADMIN only
@PostMapping("/users/search")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Operation(summary = "Search and filter users with pagination")
@Transactional(readOnly = true) // ← បន្ថែមនេះ
public ResponseEntity<?> searchUsers(@RequestBody UserSearchRequest request) {
    try {
        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 10;
        String sortBy = request.getSortBy() != null ? request.getSortBy() : "createdAt";
        String sortOrder = request.getSortOrder() != null ? request.getSortOrder() : "desc";

        Sort sort = sortOrder.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<User> users = userRepository.searchUsers(
                request.getKeyword(),
                request.getRole(),
                request.getIsEnabled(),
                pageable
        );

        List<UserProfileResponse> userResponses = users.getContent().stream()
                .map(this::mapToUserProfileResponse)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("users", userResponses);
        response.put("currentPage", users.getNumber());
        response.put("totalItems", users.getTotalElements());
        response.put("totalPages", users.getTotalPages());

        return ResponseEntity.ok(response);
    } catch (Exception e) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
    }
}
    // ១៣. Get User Details — ADMIN only
    @GetMapping("/users/{userId}/details")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get detailed user information")
    public ResponseEntity<?> getUserDetails(@PathVariable Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Map<String, Object> userDetails = new HashMap<>();
            userDetails.put("id", user.getId());
            userDetails.put("username", user.getUsername());
            userDetails.put("email", user.getEmail());
            userDetails.put("firstName", user.getFirstName());
            userDetails.put("lastName", user.getLastName());
            userDetails.put("phoneNumber", user.getPhoneNumber());
            userDetails.put("role", user.getRole());
            userDetails.put("isEnabled", user.getIsEnabled());
            userDetails.put("createdAt", user.getCreatedAt());
            userDetails.put("lastLoginAt", user.getLastLoginAt());

            // User statistics
            long userOrders = orderRepository.findByUser_IdOrderByOrderDateDesc(userId).size();
            BigDecimal totalSpent = orderRepository.findByUser_IdOrderByOrderDateDesc(userId).stream()
                    .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            userDetails.put("statistics", Map.of(
                    "totalOrders", userOrders,
                    "totalSpent", totalSpent
            ));

            return ResponseEntity.ok(userDetails);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ១៤. Bulk User Actions — ADMIN only
    @PostMapping("/users/bulk-action")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Perform bulk actions on multiple users")
    public ResponseEntity<?> bulkUserAction(@RequestBody BulkUserActionRequest request) {
        try {
            List<Long> userIds = request.getUserIds();
            String action = request.getAction();
            int successCount = 0;
            List<String> errors = new ArrayList<>();

            for (Long userId : userIds) {
                try {
                    switch (action.toLowerCase()) {
                        case "enable":
                            User enableUser = userRepository.findById(userId).orElseThrow();
                            enableUser.setIsEnabled(true);
                            userRepository.save(enableUser);
                            successCount++;
                            break;
                        case "disable":
                            User disableUser = userRepository.findById(userId).orElseThrow();
                            disableUser.setIsEnabled(false);
                            userRepository.save(disableUser);
                            successCount++;
                            break;
                        case "make-admin":
                            userService.changeUserRole(userId, "ADMIN");
                            successCount++;
                            break;
                        case "make-user":
                            userService.changeUserRole(userId, "USER");
                            successCount++;
                            break;
                        case "delete":
                            userService.deleteUser(userId);
                            successCount++;
                            break;
                        default:
                            errors.add("Unknown action: " + action);
                    }
                } catch (Exception e) {
                    errors.add("Failed to " + action + " user " + userId + ": " + e.getMessage());
                }
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Bulk action completed",
                    "action", action,
                    "successCount", successCount,
                    "failureCount", errors.size(),
                    "errors", errors
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ==========================================
    // ADVANCED ORDER MANAGEMENT
    // ==========================================

    @PostMapping("/orders/search")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Search and filter orders with pagination")
    @Transactional(readOnly = true)
    public ResponseEntity<?> searchOrders(@RequestBody OrderSearchRequest request) {
        try {
            int page = request.getPage() != null ? request.getPage() : 0;
            int size = request.getSize() != null ? request.getSize() : 10;
            String sortBy = request.getSortBy() != null ? request.getSortBy() : "orderDate";
            String sortOrder = request.getSortOrder() != null ? request.getSortOrder() : "desc";

            // ✅ Native Query ត្រូវការ snake_case column names សម្រាប់ sort
            String dbSortBy = switch (sortBy) {
                case "orderDate" -> "order_date";
                case "totalAmount" -> "total_amount";
                case "status" -> "status";
                default -> "order_date";
            };

            Sort sort = sortOrder.equalsIgnoreCase("asc") ?
                    Sort.by(dbSortBy).ascending() : Sort.by(dbSortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);

            // ✅ ប្តូរ OrderStatus → String
            String statusStr = request.getStatus() != null ?
                    request.getStatus().name() : null;

            Page<Order> orders = orderRepository.searchOrders(
                    request.getKeyword(),
                    statusStr,
                    request.getStartDate(),
                    request.getEndDate(),
                    request.getUserId(),
                    pageable
            );

            // ✅ Map to DTO មិន return Entity ផ្ទាល់ (ជៀសវាង Lazy Loading)
            List<Map<String, Object>> orderList = orders.getContent().stream()
                    .map(o -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", o.getOrderId());
                        map.put("orderDate", o.getOrderDate());
                        map.put("status", o.getStatus());
                        map.put("paymentStatus", o.getPaymentStatus());
                        map.put("paymentMethod", o.getPaymentMethod());
                        map.put("subTotal", o.getSubTotal());
                        map.put("discountAmount", o.getDiscountAmount());
                        map.put("shippingFee", o.getShippingFee());
                        map.put("tax", o.getTax());
                        map.put("totalAmount", o.getTotalAmount());
                        map.put("trackingNumber", o.getTrackingNumber());
                        map.put("username", o.getUser().getUsername());
                        map.put("email", o.getUser().getEmail());
                        return map;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("orders", orderList);
            response.put("currentPage", orders.getNumber());
            response.put("totalItems", orders.getTotalElements());
            response.put("totalPages", orders.getTotalPages());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/orders/bulk-status-update")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update status for multiple orders at once")
    @Transactional
    public ResponseEntity<?> bulkOrderStatusUpdate(
            @RequestBody Map<String, Object> request) {
        try {
            // ✅ Validate request fields
            if (request.get("orderIds") == null || request.get("status") == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "orderIds and status are required"));
            }

            @SuppressWarnings("unchecked")
            List<Integer> rawIds = (List<Integer>) request.get("orderIds");
            // ✅ Fix: Jackson deserializes numbers as Integer, not Long
            List<Long> orderIds = rawIds.stream()
                    .map(id -> Long.valueOf(id.toString()))
                    .collect(Collectors.toList());

            String newStatus = (String) request.get("status");
            String note = request.get("note") != null ? (String) request.get("note") : "";

            // ✅ Validate status value
            OrderStatus status;
            try {
                status = OrderStatus.valueOf(newStatus.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid status: " + newStatus +
                                ". Valid values: " + Arrays.toString(OrderStatus.values())));
            }

            int successCount = 0;
            List<String> errors = new ArrayList<>();

            for (Long orderId : orderIds) {
                try {
                    Order order = orderRepository.findById(orderId)
                            .orElseThrow(() -> new RuntimeException("Order " + orderId + " not found"));

                    if (order.getStatus() == status) {
                        errors.add("Order " + orderId + " is already in status " + status);
                        continue;
                    }

                    if (order.getStatus().canTransitionTo(status)) {
                        order.setStatus(status);

                        // ✅ Update timestamps based on status
                        switch (status) {
                            case SHIPPED -> order.setShippingDate(LocalDateTime.now());
                            case DELIVERED -> order.setDeliveredDate(LocalDateTime.now());
                            case PAID -> order.setPaymentDate(LocalDateTime.now());
                            default -> {}
                        }

                        orderRepository.save(order);
                        successCount++;
                    } else {
                        errors.add("Cannot transition order " + orderId +
                                " from " + order.getStatus() +
                                " to " + status +
                                ". Allowed transitions from " + order.getStatus() +
                                ": " + getAllowedTransitions(order.getStatus()));
                    }
                } catch (Exception e) {
                    errors.add("Failed to update order " + orderId + ": " + e.getMessage());
                }
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Bulk status update completed",
                    "newStatus", newStatus,
                    "successCount", successCount,
                    "failureCount", errors.size(),
                    "errors", errors
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage() != null ?
                            e.getMessage() : "An unexpected error occurred"));
        }
    }

    // ✅ Helper method — បង្ហាញ transitions ដែលអាចធ្វើ
    private String getAllowedTransitions(OrderStatus currentStatus) {
        List<String> allowed = new ArrayList<>();
        for (OrderStatus s : OrderStatus.values()) {
            if (currentStatus.canTransitionTo(s)) {
                allowed.add(s.name());
            }
        }
        return allowed.isEmpty() ? "none (terminal state)" : String.join(", ", allowed);
    }
    // ==========================================
    // PRODUCT INVENTORY MANAGEMENT
    // ==========================================

    @GetMapping("/products/low-stock")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get products with low stock",
            description = "Retrieve products that have stock quantity below the specified threshold")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of low stock products"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @Transactional(readOnly = true) // ✅ បន្ថែមនេះ
    public ResponseEntity<?> getLowStockProducts(
            @RequestParam(defaultValue = "10")
            @Parameter(description = "Stock threshold level (default: 10)", required = false)
            Integer threshold) {
        try {
            List<Map<String, Object>> lowStockProducts = productReposity.findAll().stream()
                    .filter(p -> p.getStock() != null && p.getStock() <= threshold)
                    .sorted(Comparator.comparingInt(Product::getStock))
                    .map(p -> {
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("id", p.getProId());
                        map.put("name", p.getProName());
                        map.put("sku", p.getSku());
                        map.put("stock", p.getStock());
                        map.put("price", p.getProPrice());
                        map.put("brand", p.getProBrand());
                        map.put("available", p.getAvailable());
                        map.put("discount", p.getDiscount());
                        map.put("rating", p.getRating());
                        map.put("categoryId", p.getCategoryId());
                        map.put("categoryName", p.getCategoryName());
                        // ✅ Map images ដោយខ្លួនឯង មិន return List<ProductImage> ផ្ទាល់
                        List<String> imageUrls = p.getImages() != null ?
                                p.getImages().stream()
                                .map(img -> img.getImageUrl()) // ✅ ប្តូរតាម field នៅ ProductImage
                                .collect(Collectors.toList())
                                : new ArrayList<>();
                        map.put("images", imageUrls);
                        map.put("createdAt", p.getCreatedAt());
                        map.put("updatedAt", p.getUpdatedAt());
                        return map;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "threshold", threshold,
                    "count", lowStockProducts.size(),
                    "products", lowStockProducts
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/products/out-of-stock")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get products that are out of stock",
            description = "Retrieve all products with zero stock")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of out-of-stock products"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @Transactional(readOnly = true) // ✅ បន្ថែមនេះ
    public ResponseEntity<?> getOutOfStockProducts() {
        try {
            List<Map<String, Object>> outOfStockProducts = productReposity.findAll().stream()
                    .filter(p -> p.getStock() != null && p.getStock() == 0)
                    .map(p -> {
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("id", p.getProId());
                        map.put("name", p.getProName());
                        map.put("sku", p.getSku());
                        map.put("stock", p.getStock());
                        map.put("price", p.getProPrice());
                        map.put("brand", p.getProBrand());
                        map.put("available", p.getAvailable());
                        map.put("discount", p.getDiscount());
                        map.put("rating", p.getRating());
                        map.put("categoryId", p.getCategoryId());
                        map.put("categoryName", p.getCategoryName());
                        // ✅ Map images → List<String> URLs
                        List<String> imageUrls = p.getImages() != null ?
                                p.getImages().stream()
                                .map(ProductImage::getImageUrl)
                                .collect(Collectors.toList())
                                : new ArrayList<>();
                        map.put("images", imageUrls);
                        map.put("createdAt", p.getCreatedAt());
                        map.put("updatedAt", p.getUpdatedAt());
                        return map;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "count", outOfStockProducts.size(),
                    "products", outOfStockProducts
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ១៩. Update Product Stock — ADMIN only
    @PutMapping("/products/{productId}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update product stock quantity",
            description = "Update the stock quantity for a product with optional reason")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid stock quantity"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<?> updateProductStock(
            @PathVariable
            @Parameter(description = "Product ID to update", required = true)
            Long productId,
            @RequestBody
            @Parameter(description = "Stock update request with stock quantity and reason",
                    required = true)
            Map<String, Object> request) {
        try {
            Product product = productReposity.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            Integer newStock = (Integer) request.get("stock");
            String reason = (String) request.get("reason");

            if (newStock == null || newStock < 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid stock quantity"));
            }

            Integer oldStock = product.getStock();
            product.setStock(newStock);
            product.setAvailable(newStock > 0);
            productReposity.save(product);

            return ResponseEntity.ok(Map.of(
                    "message", "Stock updated successfully",
                    "productId", productId,
                    "oldStock", oldStock,
                    "newStock", newStock,
                    "reason", reason != null ? reason : "Manual update"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ==========================================
    // ANALYTICS & REPORTS
    // ==========================================

    @GetMapping("/reports/revenue")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get revenue report for date range")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getRevenueReport(
            // ✅ ប្តូរពី LocalDateTime មក String រួច parse ខ្លួនឯង
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            // ✅ Parse String → LocalDateTime
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            LocalDateTime start = LocalDateTime.parse(startDate, formatter);
            LocalDateTime end = LocalDateTime.parse(endDate, formatter);

            BigDecimal revenue = orderRepository.calculateRevenueByDateRange(
                    start, end, OrderStatus.CANCELLED);

            List<Order> orders = orderRepository.findOrdersByDateRange(start, end);
            long totalOrders = orders.size();
            long cancelledOrders = orders.stream()
                    .filter(o -> o.getStatus() == OrderStatus.CANCELLED).count();

            BigDecimal averageOrderValue = totalOrders > 0 ?
                    revenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP) :
                    BigDecimal.ZERO;

            return ResponseEntity.ok(Map.of(
                    "startDate", start.toString(),
                    "endDate", end.toString(),
                    "totalRevenue", revenue,
                    "totalOrders", totalOrders,
                    "cancelledOrders", cancelledOrders,
                    "averageOrderValue", averageOrderValue
            ));
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid date format. Use: yyyy-MM-ddTHH:mm:ss. Example: 2026-01-01T00:00:00"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    // ២១. Get Top Selling Products — ADMIN only
    @GetMapping("/reports/top-products")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get top selling products by purchase count")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getTopSellingProducts(
            @RequestParam(defaultValue = "10") Integer limit) {
        try {
            List<Map<String, Object>> topProducts = productReposity.findAll().stream()
                    .sorted(Comparator.comparingLong(
                            (Product p) -> p.getPurchaseCount() != null ? p.getPurchaseCount() : 0
                    ).reversed())
                    .limit(limit)
                    .map(p -> {
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("id", p.getProId());
                        map.put("name", p.getProName());
                        map.put("sku", p.getSku());
                        map.put("purchaseCount", p.getPurchaseCount());
                        map.put("viewCount", p.getViewCount());
                        map.put("stock", p.getStock());
                        map.put("price", p.getProPrice());
                        map.put("brand", p.getProBrand());
                        map.put("rating", p.getRating());
                        map.put("discount", p.getDiscount());
                        map.put("available", p.getAvailable());
                        map.put("categoryId", p.getCategoryId());
                        map.put("categoryName", p.getCategoryName());
                        // ✅ Map images → List<String> URLs មិន return Entity ផ្ទាល់
                        List<String> imageUrls = p.getImages() != null ?
                                p.getImages().stream()
                                .map(ProductImage::getImageUrl)
                                .collect(Collectors.toList())
                                : new ArrayList<>();
                        map.put("images", imageUrls);
                        map.put("createdAt", p.getCreatedAt());
                        map.put("updatedAt", p.getUpdatedAt());
                        return map;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "count", topProducts.size(),
                    "products", topProducts
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ២២. Get User Statistics — ADMIN only
    @GetMapping("/reports/user-stats")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get user statistics by role and status")
    public ResponseEntity<?> getUserStatistics() {
        try {
            long totalUsers = userRepository.count();
            long adminUsers = userRepository.countByRole(Role.ADMIN);
            long regularUsers = userRepository.countByRole(Role.USER);
            long enabledUsers = userRepository.countByIsEnabled(true);
            long disabledUsers = userRepository.countByIsEnabled(false);

            return ResponseEntity.ok(Map.of(
                    "totalUsers", totalUsers,
                    "byRole", Map.of(
                            "ADMIN", adminUsers,
                            "USER", regularUsers
                    ),
                    "byStatus", Map.of(
                            "enabled", enabledUsers,
                            "disabled", disabledUsers
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Helper method
    private UserProfileResponse mapToUserProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .addresses(user.getAddresses() != null ?
                        user.getAddresses().stream()
                                .map(addr -> AddressDto.builder()
                                        .addressId(addr.getAddressId())
                                        .fullName(addr.getFullName())
                                        .phoneNumber(addr.getPhoneNumber())
                                        .city(addr.getCity())
                                        .district(addr.getDistrict())
                                        .detailsAddress(addr.getDetailsAddress())
                                        .build())
                                .collect(Collectors.toList()) :
                        List.of())
                .build();
    }
}