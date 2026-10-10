package com.mycompany.dawacare.model;

public record PatientOption(int patientId, String fullName) {
    @Override
    public String toString() {
        return fullName;
    }
}