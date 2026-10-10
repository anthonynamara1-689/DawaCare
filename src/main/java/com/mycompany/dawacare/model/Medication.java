package com.mycompany.dawacare.model;

/** medicationId is null for a new medication. usageCount = prescription items using it. */
public record Medication(Integer medicationId, Integer categoryId, String categoryName,
                         String name, String genericName, String dosageForm, String strength,
                         String manufacturer, String description, int usageCount) {
}