package com.example.demo_project_spring_boot.controller;

import com.example.demo_project_spring_boot.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final CloudinaryService cloudinaryService;

    /**
     * Upload a single image to Cloudinary
     * @param file The image file to upload
     * @param folder The folder name (optional, defaults to "uploads")
     * @return Response containing the image URL and public ID
     */
    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "uploads") String folder) {
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body("Only image files are allowed");
        }

        try {
            Map uploadResult = cloudinaryService.uploadImage(file, folder);
            
            Map<String, Object> response = new HashMap<>();
            response.put("url", uploadResult.get("secure_url"));
            response.put("publicId", uploadResult.get("public_id"));
            response.put("width", uploadResult.get("width"));
            response.put("height", uploadResult.get("height"));
            response.put("format", uploadResult.get("format"));
            response.put("size", uploadResult.get("bytes"));
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload image: " + e.getMessage());
        }
    }

    /**
     * Upload any file to Cloudinary
     * @param file The file to upload
     * @param folder The folder name (optional, defaults to "uploads")
     * @return Response containing the file URL and metadata
     */
    @PostMapping(value = "/upload-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "uploads") String folder) {
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        try {
            Map uploadResult = cloudinaryService.uploadFile(file, folder);
            
            Map<String, Object> response = new HashMap<>();
            response.put("url", uploadResult.get("secure_url"));
            response.put("publicId", uploadResult.get("public_id"));
            response.put("resourceType", uploadResult.get("resource_type"));
            response.put("format", uploadResult.get("format"));
            response.put("size", uploadResult.get("bytes"));
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload file: " + e.getMessage());
        }
    }

    /**
     * Delete a file from Cloudinary
     * @param publicId The public ID of the file to delete
     * @return Success message
     */
    @DeleteMapping("/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteFile(@RequestParam("publicId") String publicId) {
        try {
            cloudinaryService.deleteFile(publicId);
            return ResponseEntity.ok("File deleted successfully");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete file: " + e.getMessage());
        }
    }

    /**
     * Upload multiple images to Cloudinary
     * @param files The image files to upload
     * @param folder The folder name (optional, defaults to "uploads")
     * @return Response containing URLs and public IDs for all uploaded images
     */
    @PostMapping(value = "/upload-multiple-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> uploadMultipleImages(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "folder", defaultValue = "uploads") String folder) {
        
        if (files.length == 0) {
            return ResponseEntity.badRequest().body("No files provided");
        }

        try {
            Map<String, Object> responses = new HashMap<>();
            int successCount = 0;
            int failCount = 0;

            for (MultipartFile file : files) {
                if (!file.isEmpty() && file.getContentType() != null && file.getContentType().startsWith("image/")) {
                    try {
                        Map uploadResult = cloudinaryService.uploadImage(file, folder);
                        responses.put(file.getOriginalFilename(), Map.of(
                                "url", uploadResult.get("secure_url"),
                                "publicId", uploadResult.get("public_id"),
                                "status", "success"
                        ));
                        successCount++;
                    } catch (IOException e) {
                        responses.put(file.getOriginalFilename(), Map.of(
                                "status", "failed",
                                "error", e.getMessage()
                        ));
                        failCount++;
                    }
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("files", responses);
            result.put("successCount", successCount);
            result.put("failCount", failCount);

            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload images: " + e.getMessage());
        }
    }
}
