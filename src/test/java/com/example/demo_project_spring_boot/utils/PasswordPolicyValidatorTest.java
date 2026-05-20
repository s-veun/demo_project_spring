package com.example.demo_project_spring_boot.utils;

import com.example.demo_project_spring_boot.exception.InvalidPasswordException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordPolicyValidatorTest {

    private final PasswordPolicyValidator validator = new PasswordPolicyValidator();

    @Test
    void acceptsStrongPassword() {
        assertDoesNotThrow(() -> validator.validate("Strong@123"));
    }

    @Test
    void rejectsWeakPassword() {
        assertThrows(InvalidPasswordException.class, () -> validator.validate("weakpass"));
    }
}

