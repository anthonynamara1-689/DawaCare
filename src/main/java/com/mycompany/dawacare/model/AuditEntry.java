package com.mycompany.dawacare.model;

import java.time.LocalDateTime;

public record AuditEntry(long logId, LocalDateTime createdAt, String username, String role,
                         String action, String entity, Integer entityId, String targetName,
                         String details) {

    /** APPROVE_DOCTOR -> "Approve doctor" */
    public String actionText() {
        String s = action.replace('_', ' ').toLowerCase();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** e.g. "users #5 (anthony_doc)" */
    public String itemText() {
        if (entity == null) return "-";
        String s = entity + (entityId == null ? "" : " #" + entityId);
        return targetName == null ? s : s + " (" + targetName + ")";
    }
}