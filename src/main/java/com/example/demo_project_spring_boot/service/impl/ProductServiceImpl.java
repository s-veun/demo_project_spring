package com.example.demo_project_spring_boot.service.impl;

import com.example.demo_project_spring_boot.dto.NewArrivalProductRequestDTO;
import com.example.demo_project_spring_boot.dto.NewArrivalProductResponseDTO;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
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
    public Product addProduct(
            ProductRequestDTO request,
            MultipartFile imageFile,
            List<String> imageUrls
    ) throws IOException {

        String trimmedName = request.getProName().trim();

        if (productReposity.existsByProName(trimmedName)) {
            throw new DuplicateResourceException(
                    "Product Name : '" + trimmedName + "' Already Exists"
            );
        }

        Product product = new Product();

        product.setProName(trimmedName);
        product.setProDesc(request.getProDesc());
        product.setProPrice(request.getProPrice());
        product.setProBrand(request.getProBrand());
        product.setStock(request.getQuantity());
        product.setDiscount(request.getDiscount());
        product.setTags(request.getTags());

        // category
        if (request.getCategoryId() != null) {

            Category category = categoryRepository
                    .findById(request.getCategoryId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Category Not Found"
                            ));

            product.setCategory(category);
        }

        Product savedProduct = productReposity.save(product);

        // =========================================
        // PRE-UPLOADED IMAGE URLS
        // =========================================

        if (imageUrls != null && !imageUrls.isEmpty()) {

            int index = 0;

            for (String imageUrl : imageUrls) {

                if (imageUrl == null || imageUrl.isBlank()) {
                    continue;
                }

                ProductImage productImage =
                        ProductImage.builder()
                                .imageUrl(imageUrl)
                                .publicId(null)
                                .thumbnail(index == 0)
                                .sortOrder(index)
                                .product(savedProduct)
                                .build();

                savedProduct.getImages().add(productImage);

                index++;
            }
        }

        // =========================================
        // FALLBACK SINGLE IMAGE
        // =========================================

        else if (imageFile != null && !imageFile.isEmpty()) {

            Map<?, ?> uploadResult =
                    cloudinaryService.uploadImage(
                            imageFile,
                            "products"
                    );

            ProductImage productImage =
                    ProductImage.builder()
                            .imageUrl(
                                    uploadResult.get("secure_url")
                                            .toString()
                            )
                            .publicId(
                                    uploadResult.get("public_id")
                                            .toString()
                            )
                            .thumbnail(true)
                            .sortOrder(0)
                            .product(savedProduct)
                            .build();

            savedProduct.getImages().add(productImage);
        }

        return productReposity.save(savedProduct);
    }

    // ════════════════════════════════════════════════════
    // ADD MULTIPLE IMAGES TO EXISTING PRODUCT
    // ════════════════════════════════════════════════════
    @Override
    public List<ProductImage> addProductImages(
            Long productId,
            List<MultipartFile> files
    ) throws IOException {

        Product product = productReposity
                .findByIdWithImages(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product Not Found"
                        ));

        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one image is required"
            );
        }

        int currentSize = product.getImages().size();

        for (MultipartFile file : files) {

            if (file == null || file.isEmpty()) {
                continue;
            }

            String contentType = file.getContentType();

            if (contentType == null
                    || !contentType.startsWith("image/")) {
                continue;
            }

            Map<?, ?> uploadResult =
                    cloudinaryService.uploadImage(
                            file,
                            "products"
                    );

            ProductImage productImage =
                    ProductImage.builder()
                            .imageUrl(
                                    uploadResult.get("secure_url")
                                            .toString()
                            )
                            .publicId(
                                    uploadResult.get("public_id")
                                            .toString()
                            )
                            .thumbnail(product.getImages().isEmpty())
                            .sortOrder(currentSize++)
                            .product(product)
                            .build();

            product.getImages().add(productImage);
        }

        Product updatedProduct =
                productReposity.save(product);

        return updatedProduct.getImages();
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
                ProductImage oldImage = existingProduct.getImages().getFirst();
                try {
                    if (oldImage.getPublicId() != null && !oldImage.getPublicId().isBlank()) {
                        cloudinaryService.deleteFile(oldImage.getPublicId());
                    }
                    productImageRepository.delete(oldImage);
                    existingProduct.getImages().clear();
                } catch (Exception e) {
                    log.warn("[ProductService] Failed to delete old image publicId={}: {}", oldImage.getPublicId(), e.getMessage());
                }
            }

            Map<?, ?> uploadResult = cloudinaryService.uploadImage(imageFile, "products");
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
                if (image.getPublicId() != null && !image.getPublicId().isBlank()) {
                    cloudinaryService.deleteFile(image.getPublicId());
                }
            } catch (Exception e) {
                log.warn("[ProductService] Failed to delete Cloudinary image publicId={}: {}", image.getPublicId(), e.getMessage());
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

    private void populateImageInfo(Product product) {
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            product.setImageUrl(product.getImages().getFirst().getImageUrl());
            product.setImageUrls(product.getImages().stream()
                    .map(ProductImage::getImageUrl)
                    .collect(Collectors.toList()));
        }
    }

    // ════════════════════════════════════════════════════
    // ADD NEW ARRIVAL PRODUCT
    // ════════════════════════════════════════════════════
    @Override
    public NewArrivalProductResponseDTO addNewArrivalProduct(
            NewArrivalProductRequestDTO request,
            MultipartFile imageFile,
            List<String> imageUrls
    ) throws IOException {

        // Validate product name
        String trimmedName = request.getProName().trim();
        if (productReposity.existsByProName(trimmedName)) {
            throw new DuplicateResourceException(
                    "Product Name : '" + trimmedName + "' Already Exists"
            );
        }

        // Create product entity
        Product product = new Product();
        product.setProName(trimmedName);
        product.setProDesc(request.getProDesc());
        product.setProPrice(request.getProPrice());
        product.setProBrand(request.getProBrand());
        product.setStock(request.getQuantity());
        product.setDiscount(request.getDiscount());
        product.setTags(request.getTags());
        product.setAvailable(true);  // New arrivals are available by default

        // Set release date
        if (request.getReleaseDate() != null && !request.getReleaseDate().isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Date releaseDate = sdf.parse(request.getReleaseDate());
                product.setReleaseDate(releaseDate);
            } catch (Exception e) {
                product.setReleaseDate(new Date());  // Default to today
            }
        } else {
            product.setReleaseDate(new Date());  // Default to today
        }

        // Set category if provided
        if (request.getCategoryId() != null) {
            Category category = categoryRepository
                    .findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Category Not Found"
                    ));
            product.setCategory(category);
        }

        // Save product
        Product savedProduct = productReposity.save(product);

        // Handle pre-uploaded image URLs
        if (imageUrls != null && !imageUrls.isEmpty()) {
            int index = 0;
            for (String imageUrl : imageUrls) {
                if (imageUrl == null || imageUrl.isBlank()) {
                    continue;
                }
                ProductImage productImage = ProductImage.builder()
                        .imageUrl(imageUrl)
                        .publicId(null)
                        .thumbnail(index == 0)
                        .sortOrder(index)
                        .product(savedProduct)
                        .build();
                savedProduct.getImages().add(productImage);
                index++;
            }
        }
        // Fallback to single image upload
        else if (imageFile != null && !imageFile.isEmpty()) {
            Map<?, ?> uploadResult = cloudinaryService.uploadImage(imageFile, "products/arrivals");
            ProductImage productImage = ProductImage.builder()
                    .imageUrl(uploadResult.get("secure_url").toString())
                    .publicId(uploadResult.get("public_id").toString())
                    .thumbnail(true)
                    .sortOrder(0)
                    .product(savedProduct)
                    .build();
            savedProduct.getImages().add(productImage);
        }

        // Save with images
        Product finalProduct = productReposity.save(savedProduct);

        // Build and return response DTO
        return buildNewArrivalResponseDTO(finalProduct, request.getDaysToShowAsNew());
    }

    // ════════════════════════════════════════════════════
    // GET NEW ARRIVAL PRODUCTS (by days limit)
    // ════════════════════════════════════════════════════
    @Override
    public List<NewArrivalProductResponseDTO> getNewArrivalProducts(Integer daysLimit) {
        if (daysLimit == null || daysLimit <= 0) {
            daysLimit = 30;  // Default: 30 days
        }

        final Integer limit = daysLimit;  // Make it effectively final for lambda
        LocalDateTime cutoff = LocalDateTime.now().minusDays(limit.longValue());
        List<Product> products = productReposity.findNewArrivalProducts(cutoff);

        return products.stream()
                .map(product -> buildNewArrivalResponseDTO(product, limit))
                .collect(Collectors.toList());
    }

    // ════════════════════════════════════════════════════
    // GET NEW ARRIVAL PRODUCTS (with pagination)
    // ════════════════════════════════════════════════════
    @Override
    public List<NewArrivalProductResponseDTO> getNewArrivalProducts(Integer limit, Integer offset) {
        if (limit == null || limit <= 0) {
            limit = 10;  // Default: 10 items per page
        }
        if (offset == null || offset < 0) {
            offset = 0;  // Default: start from 0
        }

        Integer daysLimit = 30;  // Default: 30 days for new arrivals
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysLimit.longValue());
        List<Product> products = productReposity.findNewArrivalProductsWithPagination(cutoff, limit, offset);

        return products.stream()
                .map(product -> buildNewArrivalResponseDTO(product, daysLimit))
                .collect(Collectors.toList());
    }

    // ════════════════════════════════════════════════════
    // HELPER: Build New Arrival Response DTO
    // ════════════════════════════════════════════════════
    private NewArrivalProductResponseDTO buildNewArrivalResponseDTO(Product product, Integer daysToShowAsNew) {
        if (daysToShowAsNew == null || daysToShowAsNew <= 0) {
            daysToShowAsNew = 30;
        }

        // Calculate if product is still considered "new"
        long daysSinceCreation = ChronoUnit.DAYS.between(
                product.getCreatedAt(),
                LocalDateTime.now()
        );
        boolean isNew = daysSinceCreation <= daysToShowAsNew;

        // Populate category info
        populateCategoryInfo(product);
        populateImageInfo(product);

        // Build response
        return NewArrivalProductResponseDTO.builder()
                .proId(product.getProId())
                .proName(product.getProName())
                .sku(product.getSku())
                .proDesc(product.getProDesc())
                .proPrice(product.getProPrice())
                .proBrand(product.getProBrand())
                .discount(product.getDiscount())
                .stock(product.getStock())
                .tags(product.getTags())
                .available(product.getAvailable())
                .releaseDate(product.getReleaseDate())
                .categoryName(product.getCategoryName())
                .categoryId(product.getCategoryId())
                .imageUrl(product.getImageUrl())
                .imageUrls(product.getImageUrls())
                .arrivalNotes("Newly arrived product on " +
                        new SimpleDateFormat("yyyy-MM-dd").format(product.getReleaseDate()))
                .daysToShowAsNew(daysToShowAsNew)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .isNew(isNew)
                .weight(product.getWeight())
                .length(product.getLength())
                .width(product.getWidth())
                .height(product.getHeight())
                .build();
    }
}