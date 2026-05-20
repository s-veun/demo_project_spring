package com.example.demo_project_spring_boot.utils;

import com.example.demo_project_spring_boot.exception.InvalidPasswordException;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicyValidator {

    public void validate(String newPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            throw new InvalidPasswordException("Password must be at least 8 characters long");
        }
        if (!newPassword.matches(".*[A-Z].*")) {
            throw new InvalidPasswordException("Password must contain at least one uppercase letter");
        }
        if (!newPassword.matches(".*[a-z].*")) {
            throw new InvalidPasswordException("Password must contain at least one lowercase letter");
        }
        if (!newPassword.matches(".*[0-9].*")) {
            throw new InvalidPasswordException("Password must contain at least one number");
        }
        if (!newPassword.matches(".*[^a-zA-Z0-9].*")) {
            throw new InvalidPasswordException("Password must contain at least one special character");
        }
    }
}

