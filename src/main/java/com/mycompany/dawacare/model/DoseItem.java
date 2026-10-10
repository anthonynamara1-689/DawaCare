package com.mycompany.dawacare.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DoseItem(int doseId, LocalDateTime scheduledAt, String medication,
                       BigDecimal dosageAmount, String dosageUnit, String instructions,
                       String status, LocalDateTime takenAt, String recordedBy) {

    public String doseText() {
        return dosageAmount.stripTrailingZeros().toPlainString() + " " + dosageUnit;
    }
}