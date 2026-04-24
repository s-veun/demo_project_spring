package com.example.demo_project_spring_boot.service.impl;

import com.example.demo_project_spring_boot.exception.DuplicateResourceException;
import com.example.demo_project_spring_boot.exception.ResourceNotFoundException;
import com.example.demo_project_spring_boot.model.Product;
import com.example.demo_project_spring_boot.model.ProductImage;
import com.example.demo_project_spring_boot.repository.ProductImageRepository;
import com.example.demo_project_spring_boot.repository.ProductReposity;
import com.example.demo_project_spring_boot.service.CloudinaryService;
import com.example.demo_project_spring_boot.service.ProductService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductReposity productReposity;
    private final ProductImageRepository productImageRepository;
    private final CloudinaryService cloudinaryService;

    @Override
    public List<Product> getProducts() {
        return productReposity.findAll();
    }

    @Override
    public Product getProductById(Long proId) {
        return productReposity.findById(proId)
                .orElseThrow(() -> new ResourceNotFoundException("Product : " + proId + " Not Found"));
    }

    @Override
    public Product addProduct(Product product, MultipartFile imageFile) throws IOException {
        String trimmedName = product.getProName().trim();

        if (productReposity.existsByProName(trimmedName)){
            throw new DuplicateResourceException("Product Name : '" + trimmedName + "' Already Exists");
        }
        product.setProName(trimmedName);

        if (product.getAvailable() == null) product.setAvailable(true);
        if (product.getFavourite() == null) product.setFavourite(false);
        if (product.getRating() == null) product.setRating(0.0);

        // Save product first to get the ID
        Product savedProduct = productReposity.save(product);

        // Upload image to Cloudinary if provided
        if (imageFile != null && !imageFile.isEmpty()) {
            Map uploadResult = cloudinaryService.uploadImage(imageFile, "products");
            
            ProductImage productImage = new ProductImage();
            productImage.setImageUrl(uploadResult.get("secure_url").toString());
            productImage.setPublicId(uploadResult.get("public_id").toString());
            productImage.setProduct(savedProduct);
            
            productImageRepository.save(productImage);
            savedProduct.getImages().add(productImage);
        }

        return savedProduct;
    }

    @Override
    public Product updateProduct(Long proId, Product productDetails, MultipartFile imageFile) throws IOException {
        Product existingProduct = productReposity.findById(proId)
                .orElseThrow(() -> new ResourceNotFoundException("Product : " + proId + " Not Found"));

        String trimmedName = productDetails.getProName().trim();
        if (!existingProduct.getProName().equalsIgnoreCase(trimmedName) && productReposity.existsByProName(trimmedName)) {
            throw new DuplicateResourceException("Product :'" + trimmedName + "' Already Exists");
        }

        existingProduct.setProName(trimmedName);
        existingProduct.setProDesc(productDetails.getProDesc());
        existingProduct.setProPrice(productDetails.getProPrice());
        existingProduct.setProBrand(productDetails.getProBrand());
        existingProduct.setCategory(productDetails.getCategory());
        existingProduct.setAvailable(productDetails.getAvailable());
        existingProduct.setDiscount(productDetails.getDiscount());
        existingProduct.setStock(productDetails.getStock());
        existingProduct.setTags(productDetails.getTags());

        if (productDetails.getReleaseDate() != null) existingProduct.setReleaseDate(productDetails.getReleaseDate());
        if (productDetails.getRating() != null) existingProduct.setRating(productDetails.getRating());
        if (productDetails.getFavourite() != null) existingProduct.setFavourite(productDetails.getFavourite());

        // Upload new image to Cloudinary if provided
        if (imageFile != null && !imageFile.isEmpty()) {
            // Delete old image from Cloudinary if exists
            if (!existingProduct.getImages().isEmpty()) {
                ProductImage oldImage = existingProduct.getImages().get(0);
                try {
                    cloudinaryService.deleteFile(oldImage.getPublicId());
                    productImageRepository.delete(oldImage);
                } catch (Exception e) {
                    // Log error but continue with upload
                    System.err.println("Failed to delete old image: " + e.getMessage());
                }
            }

            // Upload new image
            Map uploadResult = cloudinaryService.uploadImage(imageFile, "products");
            
            ProductImage productImage = new ProductImage();
            productImage.setImageUrl(uploadResult.get("secure_url").toString());
            productImage.setPublicId(uploadResult.get("public_id").toString());
            productImage.setProduct(existingProduct);
            
            productImageRepository.save(productImage);
            existingProduct.getImages().add(productImage);
        }

        return productReposity.save(existingProduct);
    }

    @Override
    public void deleteProduct(Long proId) {
        Product product = productReposity.findById(proId)
                .orElseThrow(() -> new ResourceNotFoundException("Product : " + proId + " Not Found"));
        
        // Delete all images from Cloudinary
        for (ProductImage image : product.getImages()) {
            try {
                cloudinaryService.deleteFile(image.getPublicId());
            } catch (Exception e) {
                System.err.println("Failed to delete image from Cloudinary: " + e.getMessage());
            }
        }
        
        // Delete product (cascade will delete ProductImage records)
        productReposity.deleteById(proId);
    }

    @Override
    public List<Product> searchProducts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return productReposity.findAll();
        }
        return productReposity.searchProducts(keyword.trim());
    }
}