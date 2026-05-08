package com.example.demo_project_spring_boot.controller;

import com.example.demo_project_spring_boot.service.CloudinaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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

    // ════════════════════════════════════════════════════
    // ១. Upload រូបភាពតែមួយ
    // ════════════════════════════════════════════════════
    @Operation(summary = "Upload រូបភាពតែមួយ")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Upload បានជោគជ័យ",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "400", description = "File ទទេ ឬខុស format"),
            @ApiResponse(responseCode = "500", description = "Upload បរាជ័យ")
    })
    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> uploadImage(
            @RequestPart("file") MultipartFile file,
            // ✅ @RequestParam ជំនួស @RequestPart សម្រាប់ String
            @RequestParam(value = "folder", required = false, defaultValue = "uploads") String folder) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "File ទទេ — សូមជ្រើសរើស file"));
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "អនុញ្ញាតតែ image files ប៉ុណ្ណោះ (jpg, png, gif, ...)"));
        }
        try {
            Map uploadResult = cloudinaryService.uploadImage(file, folder);
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
                    .body(Map.of("error", "Upload បរាជ័យ: " + e.getMessage()));
        }
    }

    // ════════════════════════════════════════════════════
    // ២. Upload file ប្រភេទណាក៏បាន
    // ════════════════════════════════════════════════════
    @Operation(summary = "Upload file ប្រភេទណាក៏បាន (PDF, Video, ...)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Upload បានជោគជ័យ"),
            @ApiResponse(responseCode = "400", description = "File ទទេ"),
            @ApiResponse(responseCode = "500", description = "Upload បរាជ័យ")
    })
    @PostMapping(value = "/upload-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> uploadFile(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "folder", required = false, defaultValue = "uploads") String folder) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "File ទទេ — សូមជ្រើសរើស file"));
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
                    .body(Map.of("error", "Upload បរាជ័យ: " + e.getMessage()));
        }
    }

    // ════════════════════════════════════════════════════
    // ៣. លុប file — Admin តែប៉ុណ្ណោះ
    // ════════════════════════════════════════════════════
    @Operation(summary = "លុប file — Admin តែប៉ុណ្ណោះ")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "លុបបានជោគជ័យ"),
            @ApiResponse(responseCode = "500", description = "លុបបរាជ័យ")
    })
    @DeleteMapping("/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteFile(
            @Parameter(description = "Public ID របស់ file ដែលត្រូវលុប", required = true)
            @RequestParam("publicId") String publicId) {
        try {
            cloudinaryService.deleteFile(publicId);
            return ResponseEntity.ok(Map.of("message", "លុប file បានជោគជ័យ"));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "លុបបរាជ័យ: " + e.getMessage()));
        }
    }

    // ════════════════════════════════════════════════════
    // ៤. Upload រូបភាពច្រើន
    // ════════════════════════════════════════════════════
    @Operation(summary = "Upload រូបភាពច្រើនក្នុងពេលតែមួយ")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "ដំណើរការបានជោគជ័យ"),
            @ApiResponse(responseCode = "400", description = "មិនមាន file")
    })
    @PostMapping(value = "/upload-multiple-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> uploadMultipleImages(
            // ✅ List<MultipartFile> ជំនួស MultipartFile[] — Swagger render ត្រូវ
            @RequestPart("files") List<MultipartFile> files,
            @RequestParam(value = "folder", required = false, defaultValue = "uploads") String folder) {

        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "មិនមាន file — សូមជ្រើសរើស file យ៉ាងហោចណាស់មួយ"));
        }

        List<Map<String, Object>> uploadedFiles = new ArrayList<>();
        int successCount = 0;
        int failCount    = 0;

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                uploadedFiles.add(Map.of(
                        "filename", String.valueOf(file.getOriginalFilename()),
                        "status",   "failed",
                        "error",    "មិនមែន image file"
                ));
                failCount++;
                continue;
            }

            try {
                Map uploadResult = cloudinaryService.uploadImage(file, folder);
                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("filename", file.getOriginalFilename());
                fileInfo.put("url",      uploadResult.get("secure_url"));
                fileInfo.put("publicId", uploadResult.get("public_id"));
                fileInfo.put("width",    uploadResult.get("width"));
                fileInfo.put("height",   uploadResult.get("height"));
                fileInfo.put("format",   uploadResult.get("format"));
                fileInfo.put("size",     uploadResult.get("bytes"));
                fileInfo.put("status",   "success");
                uploadedFiles.add(fileInfo);
                successCount++;
            } catch (IOException e) {
                uploadedFiles.add(Map.of(
                        "filename", String.valueOf(file.getOriginalFilename()),
                        "status",   "failed",
                        "error",    e.getMessage()
                ));
                failCount++;
            }
        }

        Map<String, Object> finalResult = new HashMap<>();
        finalResult.put("files", uploadedFiles);
        finalResult.put("summary", Map.of(
                "total",   files.size(),
                "success", successCount,
                "failed",  failCount
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(finalResult);
    }
}