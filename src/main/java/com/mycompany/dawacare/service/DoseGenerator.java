package com.mycompany.dawacare.service;

import com.mycompany.dawacare.model.PrescriptionItemRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public final class DoseGenerator {

    private DoseGenerator() {}

    /** All dose date-times for the item, skipping any earlier than notBefore. */
    public static List<LocalDateTime> generate(PrescriptionItemRequest item, LocalDateTime notBefore) {
        List<LocalDateTime> doses = new ArrayList<>();
        for (LocalDate day = item.startDate();
             !day.isAfter(item.endDate());
             day = day.plusDays(item.intervalDays())) {
            for (LocalTime time : item.doseTimes()) {
                LocalDateTime at = LocalDateTime.of(day, time);
                if (!at.isBefore(notBefore)) {
                    doses.add(at);
                }
            }
        }
        return doses;
    }
}