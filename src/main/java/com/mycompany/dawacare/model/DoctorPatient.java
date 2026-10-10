package com.mycompany.dawacare.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

public record DoctorPatient(int patientId, String fullName, String gender, LocalDate dateOfBirth,
                            String phone, LocalDateTime linkedSince, int activePrescriptions) {

    public Integer age() {
        return dateOfBirth == null ? null : Period.between(dateOfBirth, LocalDate.now()).getYears();
    }
}