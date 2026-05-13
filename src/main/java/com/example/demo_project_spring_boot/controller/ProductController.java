package com.example.demo_project_spring_boot.controller;

import com.example.demo_project_spring_boot.dto.NewArrivalProductRequestDTO;
import com.example.demo_project_spring_boot.dto.NewArrivalProductResponseDTO;
import com.example.demo_project_spring_boot.dto.ProductListDTO;
import com.example.demo_project_spring_boot.dto.ProductRequestDTO;
import com.example.demo_project_spring_boot.dto.ProductResponseDTO;
import com.example.demo_project_spring_boot.model.Product;
import com.example.demo_project_spring_boot.model.ProductImage;
import com.example.demo_project_spring_boot.service.PopularityService;
import com.example.demo_project_spring_boot.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product management APIs")
public class ProductController {

    private final ProductService productService;
    private final PopularityService popularityService;

    // ════════════════════════════════════════════════════
    // 1. Get All Products — ✅ ប្រើ DTO (លឿន)
    // ════════════════════════════════════════════════════
    @GetMapping
    @Operation(summary = "Get all products")
    public ResponseEntity<List<ProductListDTO>> getAllProducts() {
        return ResponseEntity.ok(productService.getProducts());
    }

    // ════════════════════════════════════════════════════
    // 2. Get Product By ID
    // ════════════════════════════════════════════════════
    @GetMapping("/{proId}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<?> getProductById(
            @Parameter(description = "Product ID", required = true)
            @PathVariable Long proId,
            HttpServletRequest request) {
        try {
            String sessionId = request.getSession().getId();
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");
            popularityService.trackProductView(proId, sessionId, ipAddress, userAgent);
            Product product = productService.getProductById(proId);
            return ResponseEntity.ok(product);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Product not found with id: " + proId));
        }
    }

    // ════════════════════════════════════════════════════
    // 3. Add Product — form-data
    // ════════════════════════════════════════════════════
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Add new product (Admin only)",
            description = "Create a new product with multipart form-data.")
    public ResponseEntity<?> addProduct(

            @RequestParam("proName")
            @Parameter(description = "Product name (required)", required = true)
            String proName,

            @RequestParam("proDesc")
            @Parameter(description = "Product description (required)", required = true)
            String proDesc,

            @RequestParam("proPrice")
            @Parameter(description = "Product price (required)", required = true)
            BigDecimal proPrice,

            @RequestParam("proBrand")
            @Parameter(description = "Product brand (required)", required = true)
            String proBrand,

            @RequestParam("quantity")
            @Parameter(description = "Product quantity (required)", required = true)
            Integer quantity,

            @RequestParam(value = "discount", defaultValue = "0")
            @Parameter(description = "Discount percentage (optional, default: 0)")
            Double discount,

            @RequestParam(value = "tags", defaultValue = "")
            @Parameter(description = "Product tags, comma-separated (optional)")
            String tags,

            @RequestParam(value = "categoryId", required = false)
            @Parameter(description = "Category ID (optional)")
            Long categoryId,

            @RequestParam(value = "imageUrls", required = false)
            @Parameter(description = "Pre-uploaded Cloudinary image URLs (optional)")
            List<String> imageUrls,

            // ==================== ផ្នែកសំខាន់ ====================
            @RequestPart(value = "imageFile", required = false)
            @Parameter(
                    description = "Product image file (optional)",
                    schema = @Schema(type = "string", format = "binary")
            )
            MultipartFile imageFile) {

        try {
            if (proName == null || proName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Product name is required"));
            }
            // ... (validation ផ្សេងទៀត រក្សាដូចដើម)

            ProductRequestDTO dto = new ProductRequestDTO();
            dto.setProName(proName);
            dto.setProDesc(proDesc);
            dto.setProPrice(proPrice);
            dto.setProBrand(proBrand);
            dto.setQuantity(quantity);
            dto.setDiscount(discount);
            dto.setTags(tags);
            dto.setCategoryId(categoryId);

            Product savedProduct = productService.addProduct(dto, imageFile, imageUrls);

            return ResponseEntity.status(HttpStatus.CREATED).body(savedProduct);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ════════════════════════════════════════════════════
    // 3.1 Upload Multiple Product Images by Product ID
    // ════════════════════════════════════════════════════
    @PostMapping(value = "/{proId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Upload multiple product images (Admin only)")
    public ResponseEntity<?> uploadProductImages(
            @PathVariable Long proId,
            @RequestPart("files") List<MultipartFile> files) {
        try {
            List<ProductImage> images = productService.addProductImages(proId, files);

            List<Map<String, Object>> imageItems = new ArrayList<>();
            for (ProductImage image : images) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", image.getId());
                item.put("url", image.getImageUrl());
                item.put("publicId", image.getPublicId());
                imageItems.add(item);
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "productId", proId,
                    "uploadedCount", imageItems.size(),
                    "images", imageItems
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ════════════════════════════════════════════════════
    // 4. Search Products — ✅ ប្រើ DTO
    // ════════════════════════════════════════════════════
    @GetMapping("/search")
    @Operation(summary = "Search products by keyword")
    public ResponseEntity<List<ProductListDTO>> searchProducts(
            @RequestParam(value = "keyword", defaultValue = "") String keyword) {
        return ResponseEntity.ok(productService.searchProducts(keyword));
    }

    // ════════════════════════════════════════════════════
    // 5. Update Product — form-data
    // ════════════════════════════════════════════════════
    @PutMapping(value = "/{proId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Update product (Admin only) — use form-data",
            description = "Update an existing product. All fields are optional.")
    @RequestBody(
            description = "Updated product data as multipart form-data",
            required = true,
            content = @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(description = "Updated product form data")))
    public ResponseEntity<?> updateProduct(
            @PathVariable
            @Parameter(description = "Product ID to update", required = true)
            Long proId,

            @RequestParam(value = "proName", required = false)
            @Parameter(description = "Product name (optional)")
            String proName,

            @RequestParam(value = "proDesc", required = false)
            @Parameter(description = "Product description (optional)")
            String proDesc,

            @RequestParam(value = "proPrice", required = false)
            @Parameter(description = "Product price (optional)")
            BigDecimal proPrice,

            @RequestParam(value = "proBrand", required = false)
            @Parameter(description = "Product brand (optional)")
            String proBrand,

            @RequestParam(value = "quantity", required = false)
            @Parameter(description = "Product quantity (optional)")
            Integer quantity,

            @RequestParam(value = "discount", required = false)
            @Parameter(description = "Discount percentage (optional)")
            Double discount,

            @RequestParam(value = "tags", required = false)
            @Parameter(description = "Product tags, comma-separated (optional)")
            String tags,

            @RequestParam(value = "categoryId", required = false)
            @Parameter(description = "Category ID (optional)")
            Long categoryId,

            @RequestParam(value = "imageFile", required = false)
            @Parameter(description = "New product image file (optional)")
            MultipartFile imageFile) {

        try {
            // ── Validate ───────────────────────────────────────────
            if (proPrice != null && proPrice.compareTo(BigDecimal.ZERO) < 0)
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Product price cannot be negative"));
            if (quantity != null && quantity < 0)
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Product quantity cannot be negative"));

            // ── Build DTO ──────────────────────────────────────────
            ProductRequestDTO dto = new ProductRequestDTO();
            dto.setProName(proName);
            dto.setProDesc(proDesc);
            dto.setProPrice(proPrice);
            dto.setProBrand(proBrand);
            dto.setQuantity(quantity);
            dto.setDiscount(discount);
            dto.setTags(tags);
            dto.setCategoryId(categoryId);

            Product updatedProduct = productService.updateProduct(proId, dto, imageFile);
            return ResponseEntity.ok(updatedProduct);

        } catch (RuntimeException | IOException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "មានបញ្ហាក្នុងការកែប្រែផលិតផល"));
        }
    }

    // ════════════════════════════════════════════════════
    // 6. Delete Product
    // ════════════════════════════════════════════════════
    @DeleteMapping("/{proId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete product (Admin only)")
    public ResponseEntity<?> deleteProduct(@PathVariable Long proId) {
        try {
            productService.deleteProduct(proId);
            return ResponseEntity.ok(Map.of("message", "លុបផលិតផលជោគជ័យ"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "មានបញ្ហាក្នុងការលុបផលិតផល"));
        }
    }

    // ════════════════════════════════════════════════════
    // 7. New Arrival Product — Add
    // ════════════════════════════════════════════════════
    @PostMapping(value = "/new-arrivals", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Add new arrival product (Admin only)",
            description = "Create a new product marked as newly arrived with multipart form-data.",
            tags = {"Products", "New Arrivals"})
    public ResponseEntity<?> addNewArrivalProduct(
            @RequestParam("proName")
            @Parameter(description = "Product name (required)", required = true)
            String proName,

            @RequestParam("proDesc")
            @Parameter(description = "Product description (required)", required = true)
            String proDesc,

            @RequestParam("proPrice")
            @Parameter(description = "Product price (required)", required = true)
            BigDecimal proPrice,

            @RequestParam("proBrand")
            @Parameter(description = "Product brand (required)", required = true)
            String proBrand,

            @RequestParam("quantity")
            @Parameter(description = "Product quantity in stock (required)", required = true)
            Integer quantity,

            @RequestParam(value = "discount", defaultValue = "0")
            @Parameter(description = "Discount percentage (optional, default: 0)")
            Double discount,

            @RequestParam(value = "tags", defaultValue = "")
            @Parameter(description = "Product tags, comma-separated (optional)")
            String tags,

            @RequestParam(value = "categoryId", required = false)
            @Parameter(description = "Category ID (optional)")
            Long categoryId,

            @RequestParam(value = "releaseDate", required = false)
            @Parameter(description = "Release date in ISO 8601 format (yyyy-MM-dd) (optional, defaults to today)")
            String releaseDate,

            @RequestParam(value = "daysToShowAsNew", defaultValue = "30")
            @Parameter(description = "Days to show product as 'New' (optional, default: 30)")
            Integer daysToShowAsNew,

            @RequestParam(value = "imageUrls", required = false)
            @Parameter(description = "Pre-uploaded Cloudinary image URLs (optional)")
            List<String> imageUrls,

            @RequestPart(value = "imageFile", required = false)
            @Parameter(
                    description = "Product image file (optional)",
                    schema = @Schema(type = "string", format = "binary")
            )
            MultipartFile imageFile) {

        try {
            // Validate required fields
            if (proName == null || proName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Product name is required"));
            }
            if (proPrice == null || proPrice.compareTo(BigDecimal.ZERO) < 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Product price must be positive"));
            }
            if (quantity == null || quantity < 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Quantity cannot be negative"));
            }

            // Build request DTO
            NewArrivalProductRequestDTO dto = new NewArrivalProductRequestDTO();
            dto.setProName(proName);
            dto.setProDesc(proDesc);
            dto.setProPrice(proPrice);
            dto.setProBrand(proBrand);
            dto.setQuantity(quantity);
            dto.setDiscount(discount);
            dto.setTags(tags);
            dto.setCategoryId(categoryId);
            dto.setReleaseDate(releaseDate);
            dto.setDaysToShowAsNew(daysToShowAsNew);
            dto.setIsNewArrival(true);
            dto.setImageUrls(imageUrls);

            // Call service
            NewArrivalProductResponseDTO response = productService.addNewArrivalProduct(dto, imageFile, imageUrls);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "message", "New arrival product created successfully",
                    "data", response
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    // ════════════════════════════════════════════════════
    // 7.1 Get New Arrival Products — by days limit
    // ════════════════════════════════════════════════════
    @GetMapping("/new-arrivals")
    @Operation(
            summary = "Get new arrival products",
            description = "Retrieve products that arrived within the specified number of days",
            tags = {"Products", "New Arrivals"})
    public ResponseEntity<?> getNewArrivalProducts(
            @RequestParam(value = "days", defaultValue = "30")
            @Parameter(description = "Number of days to look back for arrivals (default: 30)")
            Integer days) {

        try {
            List<NewArrivalProductResponseDTO> products = productService.getNewArrivalProducts(days);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "daysLimit", days,
                    "count", products.size(),
                    "data", products
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    // ════════════════════════════════════════════════════
    // 7.2 Get New Arrival Products — with pagination
    // ════════════════════════════════════════════════════
    @GetMapping("/new-arrivals/paginated")
    @Operation(
            summary = "Get new arrival products with pagination",
            description = "Retrieve paginated new arrival products",
            tags = {"Products", "New Arrivals"})
    public ResponseEntity<?> getNewArrivalProductsPaginated(
            @RequestParam(value = "limit", defaultValue = "10")
            @Parameter(description = "Number of products per page (default: 10)")
            Integer limit,

            @RequestParam(value = "page", defaultValue = "1")
            @Parameter(description = "Page number (starts from 1, default: 1)")
            Integer page) {

        try {
            // Validate pagination parameters
            if (limit <= 0) limit = 10;
            if (page <= 0) page = 1;

            Integer offset = (page - 1) * limit;

            List<NewArrivalProductResponseDTO> products = productService.getNewArrivalProducts(limit, offset);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "page", page,
                    "limit", limit,
                    "count", products.size(),
                    "data", products
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    // ════════════════════════════════════════════════════
    // 8. Popular — Most Viewed
    // ════════════════════════════════════════════════════
    @GetMapping("/popular/most-viewed")
    @Operation(summary = "Get most viewed products")
    public ResponseEntity<?> getMostViewedProducts(
            @RequestParam(defaultValue = "10") int limit) {
        List<ProductResponseDTO> products = popularityService.getMostViewedProducts(limit);
        return ResponseEntity.ok(Map.of(
                "type", "most-viewed",
                "count", products.size(),
                "products", products));
    }

    // ════════════════════════════════════════════════════
    // 9. Popular — Most Purchased
    // ════════════════════════════════════════════════════
    @GetMapping("/popular/most-purchased")
    @Operation(summary = "Get most purchased products")
    public ResponseEntity<?> getMostPurchasedProducts(
            @RequestParam(defaultValue = "10") int limit) {
        List<ProductResponseDTO> products = popularityService.getMostPurchasedProducts(limit);
        return ResponseEntity.ok(Map.of(
                "type", "most-purchased",
                "count", products.size(),
                "products", products));
    }

    // ════════════════════════════════════════════════════
    // 10. Popular — Top Rated
    // ════════════════════════════════════════════════════
    @GetMapping("/popular/top-rated")
    @Operation(summary = "Get top rated products")
    public ResponseEntity<?> getTopRatedProducts(
            @RequestParam(defaultValue = "10") int limit) {
        List<ProductResponseDTO> products = popularityService.getTopRatedProducts(limit);
        return ResponseEntity.ok(Map.of(
                "type", "top-rated",
                "count", products.size(),
                "products", products));
    }

    // ════════════════════════════════════════════════════
    // 11. Popular — Trending
    // ════════════════════════════════════════════════════
    @GetMapping("/popular/trending")
    @Operation(summary = "Get trending products")
    public ResponseEntity<?> getTrendingProducts(
            @RequestParam(defaultValue = "10") int limit) {
        List<ProductResponseDTO> products = popularityService.getTrendingProducts(limit);
        return ResponseEntity.ok(Map.of(
                "type", "trending",
                "count", products.size(),
                "products", products));
    }

    // ════════════════════════════════════════════════════
    // 12. Analytics (Admin)
    // ════════════════════════════════════════════════════
    @GetMapping("/{proId}/analytics")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get product analytics (Admin only)")
    public ResponseEntity<?> getProductAnalytics(@PathVariable Long proId) {
        Map<String, Object> analytics = popularityService.getProductAnalytics(proId);
        return ResponseEntity.ok(analytics);
    }
}