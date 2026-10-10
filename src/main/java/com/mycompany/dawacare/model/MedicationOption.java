package com.mycompany.dawacare.model;

public record MedicationOption(int medicationId, String name, String genericName,
                               String strength, String dosageForm) {
    @Override
    public String toString() {
        return name + " " + strength + " (" + dosageForm + ")";
    }
}