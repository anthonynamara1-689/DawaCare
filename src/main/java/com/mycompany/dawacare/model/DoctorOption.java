package com.mycompany.dawacare.model;

public record DoctorOption(int doctorId, String fullName, String specialization, String hospitalName) {

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Dr. ").append(fullName);
        if (specialization != null) sb.append(" - ").append(specialization);
        if (hospitalName != null)   sb.append(" (").append(hospitalName).append(")");
        return sb.toString();
    }
}