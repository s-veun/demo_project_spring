package com.example.demo_project_spring_boot.service;

import com.example.demo_project_spring_boot.dto.ProductRequestDTO;
import com.example.demo_project_spring_boot.model.Product;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ProductService {
    List<Product> getProducts();
    Product getProductById(Long proId);
    Product addProduct(ProductRequestDTO request, MultipartFile imageFile) throws IOException;
    Product updateProduct(Long id, ProductRequestDTO request, MultipartFile imageFile) throws IOException;
    void deleteProduct(Long proId);
    List<Product> searchProducts(String keyword);
}