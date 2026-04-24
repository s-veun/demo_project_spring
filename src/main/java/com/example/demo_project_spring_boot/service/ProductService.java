package com.example.demo_project_spring_boot.service;

import com.example.demo_project_spring_boot.model.Product;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ProductService {

    List<Product> getProducts();

    Product getProductById(Long proId);

    Product addProduct(Product product, MultipartFile imageFile) throws IOException;

    Product updateProduct(Long proId, Product productDetails, MultipartFile imageFile) throws IOException;

    void deleteProduct(Long proId);

    List<Product> searchProducts(String keyword);
}