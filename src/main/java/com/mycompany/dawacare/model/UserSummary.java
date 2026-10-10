package com.mycompany.dawacare.model;

import java.time.LocalDateTime;

public record UserSummary(
        int userId,
        String username,
        String fullName,
        Role role,
        UserStatus status,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt) {
}