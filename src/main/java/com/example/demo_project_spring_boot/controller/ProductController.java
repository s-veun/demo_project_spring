package com.example.demo_project_spring_boot.controller;

import com.example.demo_project_spring_boot.dto.ProductRequestDTO;
import com.example.demo_project_spring_boot.model.Product;
import com.example.demo_project_spring_boot.service.PopularityService;
import com.example.demo_project_spring_boot.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

    @GetMapping
    @Operation(summary = "Get all products")
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getProducts());
    }

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

    // ✅ ប្រើ @RequestParam ដោយផ្ទាល់ — SprinDoc parse បានងាយ
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Add new product (Admin only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> addProduct(
            @RequestParam("proName") String proName,
            @RequestParam("proDesc") String proDesc,
            @RequestParam("proPrice") BigDecimal proPrice,
            @RequestParam("proBrand") String proBrand,
            @RequestParam("quantity") Integer quantity,
            @RequestParam(value = "discount", required = false) Double discount,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) {
        try {
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
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "មានបញ្ហាក្នុងការបន្ថែមផលិតផល"));
        }
    }

    @GetMapping("/search")
    @Operation(summary = "Search products by keyword")
    public ResponseEntity<List<Product>> searchProducts(
            @RequestParam(value = "keyword", defaultValue = "") String keyword) {
        return ResponseEntity.ok(productService.searchProducts(keyword));
    }

    @PutMapping(value = "/{proId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Update product (Admin only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> updateProduct(
            @PathVariable Long proId,
            @RequestParam("proName") String proName,
            @RequestParam("proDesc") String proDesc,
            @RequestParam("proPrice") BigDecimal proPrice,
            @RequestParam("proBrand") String proBrand,
            @RequestParam("quantity") Integer quantity,
            @RequestParam(value = "discount", required = false) Double discount,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) {
        try {
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
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "មានបញ្ហាក្នុងការកែប្រែផលិតផល"));
        }
    }

    @DeleteMapping("/{proId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Delete product (Admin only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<String> deleteProduct(@PathVariable Long proId) {
        try {
            productService.deleteProduct(proId);
            return ResponseEntity.ok("លុបផលិតផលជោគជ័យ");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("មានបញ្ហាក្នុងការលុបផលិតផល");
        }
    }

    @GetMapping("/popular/most-viewed")
    @Operation(summary = "Get most viewed products")
    public ResponseEntity<?> getMostViewedProducts(
            @RequestParam(defaultValue = "10") int limit) {
        List<Product> products = popularityService.getMostViewedProducts(limit);
        return ResponseEntity.ok(Map.of(
                "type", "most-viewed",
                "count", products.size(),
                "products", products));
    }

    @GetMapping("/popular/most-purchased")
    @Operation(summary = "Get most purchased products")
    public ResponseEntity<?> getMostPurchasedProducts(
            @RequestParam(defaultValue = "10") int limit) {
        List<Product> products = popularityService.getMostPurchasedProducts(limit);
        return ResponseEntity.ok(Map.of(
                "type", "most-purchased",
                "count", products.size(),
                "products", products));
    }

    @GetMapping("/popular/top-rated")
    @Operation(summary = "Get top rated products")
    public ResponseEntity<?> getTopRatedProducts(
            @RequestParam(defaultValue = "10") int limit) {
        List<Product> products = popularityService.getTopRatedProducts(limit);
        return ResponseEntity.ok(Map.of(
                "type", "top-rated",
                "count", products.size(),
                "products", products));
    }

    @GetMapping("/popular/trending")
    @Operation(summary = "Get trending products")
    public ResponseEntity<?> getTrendingProducts(
            @RequestParam(defaultValue = "10") int limit) {
        List<Product> products = popularityService.getTrendingProducts(limit);
        return ResponseEntity.ok(Map.of(
                "type", "trending",
                "count", products.size(),
                "products", products));
    }

    @GetMapping("/{proId}/analytics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get product analytics (Admin only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> getProductAnalytics(@PathVariable Long proId) {
        Map<String, Object> analytics = popularityService.getProductAnalytics(proId);
        return ResponseEntity.ok(analytics);
    }
}