package com.mycompany.dawacare.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MedicationLine(String medication, BigDecimal dosageAmount, String dosageUnit,
                             String times, int intervalDays, LocalDate startDate,
                             LocalDate endDate, String instructions, String doctorName) {

    public String doseText() {
        return dosageAmount.stripTrailingZeros().toPlainString() + " " + dosageUnit;
    }
}