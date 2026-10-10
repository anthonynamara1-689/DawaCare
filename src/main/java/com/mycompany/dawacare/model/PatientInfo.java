package com.mycompany.dawacare.model;

import java.time.LocalDate;
import java.time.Period;

public record PatientInfo(String fullName, String gender, LocalDate dateOfBirth, String phone) {

    public Integer age() {
        return dateOfBirth == null ? null : Period.between(dateOfBirth, LocalDate.now()).getYears();
    }
}