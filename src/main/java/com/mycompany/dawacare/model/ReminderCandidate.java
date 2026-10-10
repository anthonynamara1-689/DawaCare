package com.mycompany.dawacare.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReminderCandidate(int doseId, String patientName, String medication,
                                BigDecimal dosageAmount, String dosageUnit,
                                LocalDateTime scheduledAt) {

    public String doseText() {
        return dosageAmount.stripTrailingZeros().toPlainString() + " " + dosageUnit;
    }
}