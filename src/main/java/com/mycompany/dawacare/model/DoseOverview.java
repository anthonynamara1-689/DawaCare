package com.mycompany.dawacare.model;

import java.util.List;

public record DoseOverview(List<DoseItem> today,
                           AdherenceStats last7Days,
                           AdherenceStats last30Days,
                           List<AdherenceStats> byMedication30Days) {
}