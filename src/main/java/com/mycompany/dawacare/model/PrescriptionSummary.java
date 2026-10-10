package com.mycompany.dawacare.model;

import java.time.LocalDate;

public record PrescriptionSummary(int prescriptionId, String patientName, LocalDate date,
                                  String diagnosis, String status, int itemCount) {
}