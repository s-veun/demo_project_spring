package com.example.demo_project_spring_boot.utils;

import com.example.demo_project_spring_boot.config.ProfileImageProperties;
import com.example.demo_project_spring_boot.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class ProfileImageValidator {

    private final ProfileImageProperties properties;

    public String validateAndExtractExtension(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file is required");
        }

        if (file.getSize() > properties.getMaxFileSizeBytes()) {
            throw new BadRequestException("Image size exceeds the allowed limit");
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (properties.getAllowedExtensions().stream().noneMatch(ext -> ext.equalsIgnoreCase(extension))) {
            throw new BadRequestException("Unsupported image extension. Allowed: jpg, jpeg, png, webp");
        }

        String contentType = file.getContentType();
        if (contentType == null || properties.getAllowedMimeTypes().stream().noneMatch(type -> type.equalsIgnoreCase(contentType))) {
            throw new BadRequestException("Unsupported image MIME type");
        }

        validateMagicBytes(file, extension);
        return extension.toLowerCase(Locale.ROOT);
    }

    private String extractExtension(String fileName) {
        if (fileName == null || fileName.isBlank() || fileName.contains("..")) {
            throw new BadRequestException("Invalid file name");
        }

        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex == fileName.length() - 1) {
            throw new BadRequestException("File extension is required");
        }
        return fileName.substring(dotIndex + 1);
    }

    private void validateMagicBytes(MultipartFile file, String extension) {
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length < 12) {
                throw new BadRequestException("Invalid image file");
            }

            boolean valid = switch (extension.toLowerCase(Locale.ROOT)) {
                case "jpg", "jpeg" -> isJpeg(bytes);
                case "png" -> isPng(bytes);
                case "webp" -> isWebp(bytes);
                default -> false;
            };

            if (!valid) {
                throw new BadRequestException("Invalid image content");
            }
        } catch (IOException ex) {
            throw new BadRequestException("Unable to read uploaded image");
        }
    }

    private boolean isJpeg(byte[] bytes) {
        return bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF;
    }

    private boolean isPng(byte[] bytes) {
        return bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A;
    }

    private boolean isWebp(byte[] bytes) {
        return bytes.length >= 12
                && bytes[0] == 'R'
                && bytes[1] == 'I'
                && bytes[2] == 'F'
                && bytes[3] == 'F'
                && bytes[8] == 'W'
                && bytes[9] == 'E'
                && bytes[10] == 'B'
                && bytes[11] == 'P';
    }
}

