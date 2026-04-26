package com.example.demo_project_spring_boot.controller;

import com.example.demo_project_spring_boot.model.Product;
import com.example.demo_project_spring_boot.service.PopularityService;
import com.example.demo_project_spring_boot.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor; // 🌟 ប្រើនេះជំនួស @Autowired
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/products") // 🌟 កែមកត្រឹមនេះ ដើម្បីងាយស្រួលគ្រប់គ្រង URL
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product management APIs including CRUD operations and popularity tracking")
public class ProductController {

    private final ProductService productService;
    private final PopularityService popularityService;

    // ១. មើលផលិតផលទាំងអស់
    @GetMapping
    @Operation(summary = "Get all products", description = "Retrieve a list of all available products")
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getProducts());
    }

    // ២. មើលផលិតផលតាម ID
    @GetMapping("/{proId}")
    @Operation(summary = "Get product by ID", description = "Retrieve a single product by its ID. Automatically tracks product views.")
    public ResponseEntity<?> getProductById(
            @Parameter(description = "Product ID", required = true) @PathVariable Long proId, 
            HttpServletRequest request) {
        try {
            // Track product view
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

    // ៣. បន្ថែមផលិតផលថ្មី (Admin Only)
    @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addProduct(
            @RequestPart("products") Product product,
            @RequestPart("imageFile") MultipartFile imageFile) {
        try {
            Product savedProduct = productService.addProduct(product, imageFile);
            return new ResponseEntity<>(savedProduct, HttpStatus.CREATED);
        } catch (RuntimeException | IOException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("មានបញ្ហាក្នុងការបន្ថែមផលិតផល");
        }
    }

    // ៤. ទាញយករូបភាពផលិតផល
//    @GetMapping("/{proId}/image")
//    public ResponseEntity<byte[]> getImageByProductId(@PathVariable Long proId) {
//        Product product = productService.getProductById(proId);
//        byte[] imageFile = product.getImageDate();
//
//        if (imageFile == null) {
//            return ResponseEntity.notFound().build();
//        }
//
//        return ResponseEntity.ok()
//                .contentType(MediaType.valueOf(product.getImageType()))
//                .body(imageFile);
//    }

    // ៥. ស្វែងរកផលិតផល (Search)
    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(
            @RequestParam(value = "keyword", defaultValue = "") String keyword) {
        return ResponseEntity.ok(productService.searchProducts(keyword));
    }

    // ៦. កែប្រែផលិតផល (Admin Only)
    @PutMapping("/{proId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateProduct(
            @PathVariable Long proId,
            @RequestPart("products") Product product,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile) {
        try {
            Product updatedProduct = productService.updateProduct(proId, product, imageFile);
            return ResponseEntity.ok(updatedProduct);
        } catch (RuntimeException | IOException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("មានបញ្ហាក្នុងការកែប្រែផលិតផល");
        }
    }

    // ៧. លុបផលិតផល (Admin Only)
    @DeleteMapping("/{proId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteProduct(@PathVariable Long proId) {
        try {
            productService.deleteProduct(proId);
            return ResponseEntity.ok("លុបផលិតផលជោគជ័យ (Product deleted successfully)");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("មានបញ្ហាក្នុងការលុបផលិតផល");
        }
    }

    // ៩. ផលិតផលពេញនិយម (Popular Products)
    @GetMapping("/popular/most-viewed")
    public ResponseEntity<?> getMostViewedProducts(
            @RequestParam(defaultValue = "10") int limit) {
        List<Product> products = popularityService.getMostViewedProducts(limit);
        return ResponseEntity.ok(Map.of(
                "type", "most-viewed",
                "count", products.size(),
                "products", products
        ));
    }

    @GetMapping("/popular/most-purchased")
    public ResponseEntity<?> getMostPurchasedProducts(
            @RequestParam(defaultValue = "10") int limit) {
        List<Product> products = popularityService.getMostPurchasedProducts(limit);
        return ResponseEntity.ok(Map.of(
                "type", "most-purchased",
                "count", products.size(),
                "products", products
        ));
    }

    @GetMapping("/popular/top-rated")
    public ResponseEntity<?> getTopRatedProducts(
            @RequestParam(defaultValue = "10") int limit) {
        List<Product> products = popularityService.getTopRatedProducts(limit);
        return ResponseEntity.ok(Map.of(
                "type", "top-rated",
                "count", products.size(),
                "products", products
        ));
    }

    @GetMapping("/popular/trending")
    public ResponseEntity<?> getTrendingProducts(
            @RequestParam(defaultValue = "10") int limit) {
        List<Product> products = popularityService.getTrendingProducts(limit);
        return ResponseEntity.ok(Map.of(
                "type", "trending",
                "count", products.size(),
                "products", products
        ));
    }

    @GetMapping("/{proId}/analytics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getProductAnalytics(@PathVariable Long proId) {
        Map<String, Object> analytics = popularityService.getProductAnalytics(proId);
        return ResponseEntity.ok(analytics);
    }
}