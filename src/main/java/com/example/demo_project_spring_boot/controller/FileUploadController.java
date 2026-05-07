package com.example.demo_project_spring_boot.controller;

import com.example.demo_project_spring_boot.dto.MultipleImageUploadRequest;
import com.example.demo_project_spring_boot.dto.SingleImageUploadRequest;
import com.example.demo_project_spring_boot.service.CloudinaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Tag(name = "File Upload", description = "APIs for uploading and managing files on Cloudinary")
@SecurityRequirement(name = "bearerAuth")
public class FileUploadController {

    private final CloudinaryService cloudinaryService;

    // ─────────────────────────────────────────────────────────────
    // Upload single image
    // ─────────────────────────────────────────────────────────────
    @Operation(summary = "Upload a single image")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or empty file"),
            @ApiResponse(responseCode = "500", description = "Upload failed")
    })
    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> uploadImage(
            // ✅ FIX: ប្រើ @ModelAttribute ជំនួស @RequestParam
            @ModelAttribute SingleImageUploadRequest request) {

        MultipartFile file   = request.getFile();
        String        folder = request.getFolder();

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body("Only image files are allowed");
        }
        try {
            Map<String, Object> uploadResult = cloudinaryService.uploadImage(file, folder);
            Map<String, Object> response = new HashMap<>();
            response.put("url",      uploadResult.get("secure_url"));
            response.put("publicId", uploadResult.get("public_id"));
            response.put("width",    uploadResult.get("width"));
            response.put("height",   uploadResult.get("height"));
            response.put("format",   uploadResult.get("format"));
            response.put("size",     uploadResult.get("bytes"));
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload image: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Upload any file
    // ─────────────────────────────────────────────────────────────
    @Operation(summary = "Upload any file type")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Empty file"),
            @ApiResponse(responseCode = "500", description = "Upload failed")
    })
    @PostMapping(value = "/upload-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> uploadFile(
            // ✅ FIX: ប្រើ @ModelAttribute ជំនួស @RequestParam
            @ModelAttribute SingleImageUploadRequest request) {

        MultipartFile file   = request.getFile();
        String        folder = request.getFolder();

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }
        try {
            Map uploadResult = cloudinaryService.uploadFile(file, folder);
            Map<String, Object> response = new HashMap<>();
            response.put("url",          uploadResult.get("secure_url"));
            response.put("publicId",     uploadResult.get("public_id"));
            response.put("resourceType", uploadResult.get("resource_type"));
            response.put("format",       uploadResult.get("format"));
            response.put("size",         uploadResult.get("bytes"));
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload file: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Delete file
    // ─────────────────────────────────────────────────────────────
    @Operation(summary = "Delete a file — Admin only")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "500", description = "Delete failed")
    })
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

    // ─────────────────────────────────────────────────────────────
    // Upload multiple images  ← ស្នូលនៃការដោះស្រាយ
    // ─────────────────────────────────────────────────────────────
    @Operation(summary = "Upload multiple images")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "No files provided"),
            @ApiResponse(responseCode = "500", description = "Upload failed")
    })
    @PostMapping(value = "/upload-multiple-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> uploadMultipleImages(
            @ModelAttribute MultipleImageUploadRequest request) {

        MultipartFile[] files  = request.getFiles();
        String          folder = request.getFolder();

        if (files == null || files.length == 0) {
            return ResponseEntity.badRequest().body("No files provided");
        }

        try {
            // ✅ FIX: ប្រើ List ជំនួស Map — avoid filename collision
            List<Map<String, Object>> uploadedFiles = new ArrayList<>();
            int successCount = 0;
            int failCount    = 0;

            for (MultipartFile file : files) {
                if (!file.isEmpty()
                        && file.getContentType() != null
                        && file.getContentType().startsWith("image/")) {
                    try {
                        Map uploadResult = cloudinaryService.uploadImage(file, folder);

                        Map<String, Object> fileInfo = new HashMap<>();
                        // ✅ រក្សា filename + index ដើម្បី unique
                        fileInfo.put("filename",  file.getOriginalFilename());
                        fileInfo.put("url",       uploadResult.get("secure_url"));
                        fileInfo.put("publicId",  uploadResult.get("public_id"));
                        fileInfo.put("size",      uploadResult.get("bytes"));
                        fileInfo.put("format",    uploadResult.get("format"));
                        fileInfo.put("status",    "success");

                        uploadedFiles.add(fileInfo);
                        successCount++;

                    } catch (IOException e) {
                        Map<String, Object> fileInfo = new HashMap<>();
                        fileInfo.put("filename", file.getOriginalFilename());
                        fileInfo.put("status",   "failed");
                        fileInfo.put("error",    e.getMessage());

                        uploadedFiles.add(fileInfo);
                        failCount++;
                    }
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("files",        uploadedFiles);  // ✅ List — បង្ហាញគ្រប់ files
            result.put("successCount", successCount);
            result.put("failCount",    failCount);

            return ResponseEntity.status(HttpStatus.CREATED).body(result);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload images: " + e.getMessage());
        }
    }
}