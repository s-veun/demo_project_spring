package com.example.demo_project_spring_boot.service.impl;

import com.example.demo_project_spring_boot.dto.ProductListDTO;
import com.example.demo_project_spring_boot.dto.ProductRequestDTO;
import com.example.demo_project_spring_boot.exception.DuplicateResourceException;
import com.example.demo_project_spring_boot.exception.ResourceNotFoundException;
import com.example.demo_project_spring_boot.model.Category;
import com.example.demo_project_spring_boot.model.Product;
import com.example.demo_project_spring_boot.model.ProductImage;
import com.example.demo_project_spring_boot.repository.CategoryRepository;
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
    private final CategoryRepository categoryRepository;

    // ════════════════════════════════════════════════════
    // GET ALL — DTO (លឿន, 1 query)
    // ════════════════════════════════════════════════════
    @Override
    public List<ProductListDTO> getProducts() {
        return productReposity.findAllAsDTO();
    }

    // ════════════════════════════════════════════════════
    // GET BY ID — Entity (detail page)
    // ════════════════════════════════════════════════════
    @Override
    public Product getProductById(Long proId) {
        Product product = productReposity.findByIdWithCategory(proId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product : " + proId + " Not Found"));
        populateCategoryInfo(product);
        return product;
    }

    // ════════════════════════════════════════════════════
    // ADD PRODUCT
    // ════════════════════════════════════════════════════
    @Override
    public Product addProduct(ProductRequestDTO request, MultipartFile imageFile) throws IOException {
        String trimmedName = request.getProName().trim();

        if (productReposity.existsByProName(trimmedName)) {
            throw new DuplicateResourceException(
                    "Product Name : '" + trimmedName + "' Already Exists");
        }

        Product product = new Product();
        product.setProName(trimmedName);
        product.setProDesc(request.getProDesc());
        product.setProPrice(request.getProPrice());
        product.setProBrand(request.getProBrand());
        product.setStock(request.getQuantity());
        product.setDiscount(request.getDiscount());
        product.setTags(request.getTags());
        product.setAvailable(true);
        product.setFavourite(false);
        product.setRating(0.0);

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Category : " + request.getCategoryId() + " Not Found"));
            product.setCategory(category);
        }

        Product savedProduct = productReposity.save(product);
        populateCategoryInfo(savedProduct);

        if (imageFile != null && !imageFile.isEmpty()) {
            Map uploadResult = cloudinaryService.uploadImage(imageFile, "products");
            String uploadedUrl = uploadResult.get("secure_url").toString();
            String publicId    = uploadResult.get("public_id").toString();

            ProductImage productImage = new ProductImage();
            productImage.setImageUrl(uploadedUrl);
            productImage.setPublicId(publicId);
            productImage.setProduct(savedProduct);
            productImageRepository.save(productImage);

            savedProduct.getImages().add(productImage);
            savedProduct.setImageUrl(uploadedUrl);
            savedProduct.setImageUrls(List.of(uploadedUrl));
        }

        return savedProduct;
    }

    // ════════════════════════════════════════════════════
    // UPDATE PRODUCT
    // ════════════════════════════════════════════════════
    @Override
    public Product updateProduct(Long id, ProductRequestDTO request, MultipartFile imageFile)
            throws IOException {

        Product existingProduct = productReposity.findByIdWithCategory(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product : " + id + " Not Found"));

        if (request.getProName() != null) {
            String trimmedName = request.getProName().trim();
            if (!existingProduct.getProName().equalsIgnoreCase(trimmedName)
                    && productReposity.existsByProName(trimmedName)) {
                throw new DuplicateResourceException(
                        "Product : '" + trimmedName + "' Already Exists");
            }
            existingProduct.setProName(trimmedName);
        }

        if (request.getProDesc()  != null) existingProduct.setProDesc(request.getProDesc());
        if (request.getProPrice() != null) existingProduct.setProPrice(request.getProPrice());
        if (request.getProBrand() != null) existingProduct.setProBrand(request.getProBrand());
        if (request.getQuantity() != null) existingProduct.setStock(request.getQuantity());
        if (request.getDiscount() != null) existingProduct.setDiscount(request.getDiscount());
        if (request.getTags()     != null) existingProduct.setTags(request.getTags());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Category : " + request.getCategoryId() + " Not Found"));
            existingProduct.setCategory(category);
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            if (!existingProduct.getImages().isEmpty()) {
                ProductImage oldImage = existingProduct.getImages().get(0);
                try {
                    cloudinaryService.deleteFile(oldImage.getPublicId());
                    productImageRepository.delete(oldImage);
                    existingProduct.getImages().clear();
                } catch (Exception e) {
                    System.err.println("Failed to delete old image: " + e.getMessage());
                }
            }

            Map uploadResult = cloudinaryService.uploadImage(imageFile, "products");
            String uploadedUrl = uploadResult.get("secure_url").toString();
            String publicId    = uploadResult.get("public_id").toString();

            ProductImage productImage = new ProductImage();
            productImage.setImageUrl(uploadedUrl);
            productImage.setPublicId(publicId);
            productImage.setProduct(existingProduct);
            productImageRepository.save(productImage);

            existingProduct.getImages().add(productImage);
            existingProduct.setImageUrl(uploadedUrl);
            existingProduct.setImageUrls(List.of(uploadedUrl));
        }

        Product updatedProduct = productReposity.save(existingProduct);
        populateCategoryInfo(updatedProduct);
        return updatedProduct;
    }

    // ════════════════════════════════════════════════════
    // DELETE PRODUCT
    // ════════════════════════════════════════════════════
    @Override
    public void deleteProduct(Long proId) {
        Product product = productReposity.findByIdWithImages(proId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product : " + proId + " Not Found"));

        for (ProductImage image : product.getImages()) {
            try {
                cloudinaryService.deleteFile(image.getPublicId());
            } catch (Exception e) {
                System.err.println("Failed to delete image: " + e.getMessage());
            }
        }

        productReposity.deleteById(proId);
    }

    // ════════════════════════════════════════════════════
    // SEARCH — DTO
    // ════════════════════════════════════════════════════
    @Override
    public List<ProductListDTO> searchProducts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getProducts();
        }
        return productReposity.searchAsDTO(keyword.trim());
    }

    // ════════════════════════════════════════════════════
    // HELPER
    // ════════════════════════════════════════════════════
    private void populateCategoryInfo(Product product) {
        if (product.getCategory() != null) {
            product.setCategoryId(product.getCategory().getCatId());
            product.setCategoryName(product.getCategory().getCatName());
        }
    }
}