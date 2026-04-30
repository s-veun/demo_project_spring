package com.example.demo_project_spring_boot.controller;

import com.example.demo_project_spring_boot.dto.ProductListDTO;
import com.example.demo_project_spring_boot.dto.ProductRequestDTO;
import com.example.demo_project_spring_boot.dto.ProductResponseDTO;
import com.example.demo_project_spring_boot.model.Product;
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
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Add new product (Admin only) — use form-data",
            description = "Create a new product with multipart form-data.")
    @RequestBody(
            description = "Product data as multipart form-data",
            required = true,
            content = @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(description = "Product form data")))
    public ResponseEntity<?> addProduct(
            @RequestParam(value = "proName", required = false)
            @Parameter(description = "Product name (required)", required = true)
            String proName,

            @RequestParam(value = "proDesc", required = false)
            @Parameter(description = "Product description (required)", required = true)
            String proDesc,

            @RequestParam(value = "proPrice", required = false)
            @Parameter(description = "Product price (required)", required = true)
            BigDecimal proPrice,

            @RequestParam(value = "proBrand", required = false)
            @Parameter(description = "Product brand (required)", required = true)
            String proBrand,

            @RequestParam(value = "quantity", required = false)
            @Parameter(description = "Product quantity (required)", required = true)
            Integer quantity,

            @RequestParam(value = "discount", required = false, defaultValue = "0")
            @Parameter(description = "Discount percentage (optional, default: 0)")
            Double discount,

            @RequestParam(value = "tags", required = false, defaultValue = "")
            @Parameter(description = "Product tags, comma-separated (optional)")
            String tags,

            @RequestParam(value = "categoryId", required = false)
            @Parameter(description = "Category ID (optional)")
            Long categoryId,

            @RequestParam(value = "imageFile", required = false)
            @Parameter(description = "Product image file (optional)")
            MultipartFile imageFile) {

        try {
            // ── Validate ───────────────────────────────────────────
            if (proName == null || proName.trim().isEmpty())
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Product name is required"));
            if (proDesc == null || proDesc.trim().isEmpty())
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Product description is required"));
            if (proPrice == null)
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Product price is required"));
            if (proBrand == null || proBrand.trim().isEmpty())
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Product brand is required"));
            if (quantity == null)
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Product quantity is required"));
            if (proPrice.compareTo(BigDecimal.ZERO) < 0)
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Product price cannot be negative"));
            if (quantity < 0)
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

            Product savedProduct = productService.addProduct(dto, imageFile);
            return new ResponseEntity<>(savedProduct, HttpStatus.CREATED);

        } catch (RuntimeException | IOException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "មានបញ្ហាក្នុងការបន្ថែមផលិតផល"));
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
    // 7. Popular — Most Viewed
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
    // 8. Popular — Most Purchased
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
    // 9. Popular — Top Rated
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
    // 10. Popular — Trending
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
    // 11. Analytics (Admin)
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