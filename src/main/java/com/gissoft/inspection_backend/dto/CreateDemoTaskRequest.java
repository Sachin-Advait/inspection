package com.gissoft.inspection_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record CreateDemoTaskRequest(

        // ── EntityMaster fields ───────────────────────────────────────────────
        @NotBlank String entityName,        // maps to name
        @NotBlank String ownerName,
        String ownerPhone,
        @NotBlank String directorate,       // HEALTH | TECHNICAL
        @NotBlank String category,          // Restaurant | Salon | Building | Road …
        Double lat,
        Double lon,
        String complianceFlag,    // ACTIVE | WARNING | SUSPENDED (default ACTIVE)
        OffsetDateTime nextDueAt, // optional — when next inspection is due

        // ── Task fields ───────────────────────────────────────────────────────
        @NotBlank String taskType,          // INSPECTION | REINSPECTION | FOLLOW_UP
        @NotBlank String phase,
        @NotBlank String Subtype,  // Routine | Foundation | Traffic …
        @NotBlank String assignedTo,        // inspector username
        @NotNull  OffsetDateTime dueAt,
        String priority           // LOW | MEDIUM | HIGH | CRITICAL (default MEDIUM)
) {}