package com.mycompany.dawacare.model;

import java.time.LocalDateTime;

public record PendingDoctor(
        int userId,
        String fullName,
        String username,
        String email,
        String phone,
        String licenseNumber,
        String specialization,
        String hospitalName,
        LocalDateTime registeredAt) {
}