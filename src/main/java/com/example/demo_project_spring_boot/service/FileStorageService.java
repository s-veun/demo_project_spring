package com.example.demo_project_spring_boot.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    StoredImage storeProfileImage(MultipartFile file, String username, String extension);
    void deleteProfileImage(String imageName);
}

