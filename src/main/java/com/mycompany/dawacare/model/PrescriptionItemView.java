package com.mycompany.dawacare.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PrescriptionItemView(String medication, BigDecimal dosageAmount, String dosageUnit,
                                   String times, int intervalDays, LocalDate startDate,
                                   LocalDate endDate, String instructions, String status) {

    public String doseText() {
        return dosageAmount.stripTrailingZeros().toPlainString() + " " + dosageUnit;
    }
}