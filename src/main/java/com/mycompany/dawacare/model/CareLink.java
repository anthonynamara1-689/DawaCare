package com.mycompany.dawacare.model;

import java.time.LocalDateTime;

public record CareLink(
        int linkId,
        String name,
        String phone,
        String info,          // specialization/hospital, or relationship
        String status,
        LocalDateTime since) {
}