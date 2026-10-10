package com.mycompany.dawacare.model;

public record RegistrationRequest(
        Role role,
        String fullName,
        String username,
        String password,
        String confirmPassword,
        String email,
        String phone,
        // patient fields
        String dateOfBirth,
        String gender,
        String address,
        String emergencyContactName,
        String emergencyContactPhone,
        // doctor fields
        String licenseNumber,
        String specialization,
        String hospitalName) {
}