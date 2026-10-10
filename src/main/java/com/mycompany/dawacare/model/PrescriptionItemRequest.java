package com.mycompany.dawacare.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

public record PrescriptionItemRequest(
        MedicationOption medication,
        BigDecimal dosageAmount,
        String dosageUnit,
        List<LocalTime> doseTimes,
        int intervalDays,
        LocalDate startDate,
        LocalDate endDate,
        String instructions) {

    public String doseText() {
        return dosageAmount.stripTrailingZeros().toPlainString() + " " + dosageUnit;
    }

    public String timesText() {
        return doseTimes.stream().map(LocalTime::toString).collect(Collectors.joining(", "));
    }

    public static String repeatText(int intervalDays) {
        return switch (intervalDays) {
            case 1 -> "Every day";
            case 7 -> "Once a week";
            default -> "Every " + intervalDays + " days";
        };
    }
}