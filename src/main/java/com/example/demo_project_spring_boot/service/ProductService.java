package com.example.demo_project_spring_boot.service;

import com.example.demo_project_spring_boot.dto.NewArrivalProductRequestDTO;
import com.example.demo_project_spring_boot.dto.NewArrivalProductResponseDTO;
import com.example.demo_project_spring_boot.dto.ProductListDTO;
import com.example.demo_project_spring_boot.dto.ProductRequestDTO;
import com.example.demo_project_spring_boot.model.Product;
import com.example.demo_project_spring_boot.model.ProductImage;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ProductService {
    List<ProductListDTO> getProducts();                  // ✅ DTO
    Product getProductById(Long proId);
    Product addProduct(ProductRequestDTO request, MultipartFile imageFile, List<String> imageUrls) throws IOException;
    List<ProductImage> addProductImages(Long productId, List<MultipartFile> files) throws IOException;
    Product updateProduct(Long id, ProductRequestDTO request, MultipartFile imageFile) throws IOException;
    void deleteProduct(Long proId);
    List<ProductListDTO> searchProducts(String keyword); // ✅ DTO

    // ════════════════════════════════════════════════════
    // NEW ARRIVAL PRODUCT METHODS
    // ════════════════════════════════════════════════════
    NewArrivalProductResponseDTO addNewArrivalProduct(
            NewArrivalProductRequestDTO request,
            MultipartFile imageFile,
            List<String> imageUrls
    ) throws IOException;

    List<NewArrivalProductResponseDTO> getNewArrivalProducts(Integer daysLimit);

    List<NewArrivalProductResponseDTO> getNewArrivalProducts(Integer limit, Integer offset);
}