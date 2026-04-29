package com.example.demo_project_spring_boot.service.impl;

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

    // ════════════════════════════════════════════════════════
    // GET ALL PRODUCTS
    // ════════════════════════════════════════════════════════
    @Override
    public List<Product> getProducts() {
        List<Product> products = productReposity.findAllWithCategory();
        // @PostLoad នឹង populate imageUrl ដោយស្វ័យប្រវត្តិ
        // គ្រាន់តែ populate category info ប៉ុណ្ណោះ
        products.forEach(this::populateCategoryInfo);
        return products;
    }

    // ════════════════════════════════════════════════════════
    // GET PRODUCT BY ID
    // ════════════════════════════════════════════════════════
    @Override
    public Product getProductById(Long proId) {
        Product product = productReposity.findByIdWithCategory(proId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product : " + proId + " Not Found"));
        // @PostLoad នឹង populate imageUrl ដោយស្វ័យប្រវត្តិ
        populateCategoryInfo(product);
        return product;
    }

    // ════════════════════════════════════════════════════════
    // ADD PRODUCT
    // ════════════════════════════════════════════════════════
    @Override
    public Product addProduct(ProductRequestDTO request, MultipartFile imageFile) throws IOException {
        String trimmedName = request.getProName().trim();

        // ── ត្រួតពិនិត្យឈ្មោះផលិតផលស្ទួន ─────────────────────────
        if (productReposity.existsByProName(trimmedName)) {
            throw new DuplicateResourceException(
                    "Product Name : '" + trimmedName + "' Already Exists");
        }

        // ── បង្កើត Product ─────────────────────────────────────────
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

        // ── កំណត់ Category ─────────────────────────────────────────
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Category : " + request.getCategoryId() + " Not Found"));
            product.setCategory(category);
        }

        // ── រក្សាទុក Product ───────────────────────────────────────
        Product savedProduct = productReposity.save(product);
        populateCategoryInfo(savedProduct);

        // ── ★ FIX: Upload រូបភាព → Set imageUrl លើ response ──────
        if (imageFile != null && !imageFile.isEmpty()) {
            // Upload ទៅ Cloudinary
            Map uploadResult = cloudinaryService.uploadImage(imageFile, "products");

            String uploadedUrl = uploadResult.get("secure_url").toString();
            String publicId    = uploadResult.get("public_id").toString();

            // រក្សាទុក ProductImage entity
            ProductImage productImage = new ProductImage();
            productImage.setImageUrl(uploadedUrl);
            productImage.setPublicId(publicId);
            productImage.setProduct(savedProduct);
            productImageRepository.save(productImage);

            // បន្ថែមទៅ images list
            savedProduct.getImages().add(productImage);

            // ★ KEY: set imageUrl ដោយផ្ទាល់ដើម្បីបង្ហាញក្នុង
            //        response JSON ភ្លាមៗ (មុន @PostLoad)
            savedProduct.setImageUrl(uploadedUrl);
            savedProduct.setImageUrls(List.of(uploadedUrl));
        }

        return savedProduct;
    }

    // ════════════════════════════════════════════════════════
    // UPDATE PRODUCT
    // ════════════════════════════════════════════════════════
    @Override
    public Product updateProduct(Long id, ProductRequestDTO request, MultipartFile imageFile)
            throws IOException {

        // ── ស្វែងរក Product ────────────────────────────────────────
        Product existingProduct = productReposity.findByIdWithCategory(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product : " + id + " Not Found"));

        // ── Update fields (partial update — null = មិន update) ─────
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

        // ── Update Category ────────────────────────────────────────
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Category : " + request.getCategoryId() + " Not Found"));
            existingProduct.setCategory(category);
        }

        // ── ★ FIX: Replace រូបភាព → Set imageUrl លើ response ─────
        if (imageFile != null && !imageFile.isEmpty()) {

            // លុបរូបភាពចាស់ពី Cloudinary និង DB
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

            // Upload រូបភាពថ្មី
            Map uploadResult = cloudinaryService.uploadImage(imageFile, "products");

            String uploadedUrl = uploadResult.get("secure_url").toString();
            String publicId    = uploadResult.get("public_id").toString();

            // រក្សាទុក ProductImage entity
            ProductImage productImage = new ProductImage();
            productImage.setImageUrl(uploadedUrl);
            productImage.setPublicId(publicId);
            productImage.setProduct(existingProduct);
            productImageRepository.save(productImage);

            existingProduct.getImages().add(productImage);

            // ★ KEY: set imageUrl ដោយផ្ទាល់ដើម្បីបង្ហាញក្នុង
            //        response JSON ភ្លាមៗ (មុន @PostLoad)
            existingProduct.setImageUrl(uploadedUrl);
            existingProduct.setImageUrls(List.of(uploadedUrl));
        }

        // ── Save និង return ────────────────────────────────────────
        Product updatedProduct = productReposity.save(existingProduct);
        populateCategoryInfo(updatedProduct);
        return updatedProduct;
    }

    // ════════════════════════════════════════════════════════
    // DELETE PRODUCT
    // ════════════════════════════════════════════════════════
    @Override
    public void deleteProduct(Long proId) {
        Product product = productReposity.findById(proId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product : " + proId + " Not Found"));

        // លុបរូបភាពទាំងអស់ពី Cloudinary មុន
        for (ProductImage image : product.getImages()) {
            try {
                cloudinaryService.deleteFile(image.getPublicId());
            } catch (Exception e) {
                System.err.println("Failed to delete image: " + e.getMessage());
            }
        }

        productReposity.deleteById(proId);
    }

    // ════════════════════════════════════════════════════════
    // SEARCH PRODUCTS
    // ════════════════════════════════════════════════════════
    @Override
    public List<Product> searchProducts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getProducts();
        }
        List<Product> products = productReposity.searchProductsWithCategory(keyword.trim());
        // @PostLoad នឹង populate imageUrl ដោយស្វ័យប្រវត្តិ
        products.forEach(this::populateCategoryInfo);
        return products;
    }

    // ════════════════════════════════════════════════════════
    // HELPER
    // ════════════════════════════════════════════════════════
    /**
     * Populate transient category fields សម្រាប់ JSON serialization
     */
    private void populateCategoryInfo(Product product) {
        if (product.getCategory() != null) {
            product.setCategoryId(product.getCategory().getCatId());
            product.setCategoryName(product.getCategory().getCatName());
        }
    }
}