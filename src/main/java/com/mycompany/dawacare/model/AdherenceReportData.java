package com.mycompany.dawacare.model;

import java.time.LocalDateTime;
import java.util.List;

public record AdherenceReportData(
        PatientInfo patient,
        LocalDateTime periodStart,
        LocalDateTime periodEnd,
        LocalDateTime generatedAt,
        String generatedBy,
        AdherenceStats overall,
        List<AdherenceStats> byMedication,
        List<MedicationLine> currentMedications,
        List<DoseItem> history) {
}