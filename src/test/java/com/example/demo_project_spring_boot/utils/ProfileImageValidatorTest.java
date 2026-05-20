package com.example.demo_project_spring_boot.utils;

import com.example.demo_project_spring_boot.config.ProfileImageProperties;
import com.example.demo_project_spring_boot.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProfileImageValidatorTest {

    private ProfileImageValidator validator;

    @BeforeEach
    void setUp() {
        ProfileImageProperties properties = new ProfileImageProperties();
        properties.setMaxFileSizeBytes(1024 * 1024);
        validator = new ProfileImageValidator(properties);
    }

    @Test
    void acceptsValidPng() {
        byte[] png = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", png);

        String ext = validator.validateAndExtractExtension(file);

        assertEquals("png", ext);
    }

    @Test
    void rejectsExecutableDisguisedAsPng() {
        byte[] fake = "#!/bin/bash".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", fake);

        assertThrows(BadRequestException.class, () -> validator.validateAndExtractExtension(file));
    }

    @Test
    void rejectsOversizedImage() {
        byte[] payload = new byte[2 * 1024 * 1024];
        payload[0] = (byte) 0xFF;
        payload[1] = (byte) 0xD8;
        payload[2] = (byte) 0xFF;
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", payload);

        assertThrows(BadRequestException.class, () -> validator.validateAndExtractExtension(file));
    }
}

