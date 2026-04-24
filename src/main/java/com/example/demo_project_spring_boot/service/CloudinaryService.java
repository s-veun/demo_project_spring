package com.example.demo_project_spring_boot.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /**
     * Upload file to Cloudinary
     * @param file The multipart file to upload
     * @param folder The folder name in Cloudinary (e.g., "products", "users", etc.)
     * @return Map containing url, public_id, and other metadata
     */
    public Map uploadFile(MultipartFile file, String folder) throws IOException {
        return cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", folder,
                "resource_type", "auto"
        ));
    }

    /**
     * Upload image to Cloudinary
     * @param file The image file to upload
     * @param folder The folder name in Cloudinary
     * @return Map containing url, public_id, and other metadata
     */
    public Map uploadImage(MultipartFile file, String folder) throws IOException {
        return cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", folder,
                "resource_type", "image"
        ));
    }

    /**
     * Delete file from Cloudinary by public_id
     * @param publicId The public ID of the file to delete
     * @return Result map from Cloudinary
     */
    public Map deleteFile(String publicId) throws IOException {
        return cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }

    /**
     * Get Cloudinary URL from public_id
     * @param publicId The public ID
     * @return Full URL to the file
     */
    public String getUrl(String publicId) {
        return cloudinary.url().generate(publicId);
    }
}
