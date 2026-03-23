package com.gissoft.inspection_backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;

import java.util.UUID;

@Entity
@Data
public class PhaseConfig {

    @Id
    @GeneratedValue
    private UUID id;

    private String directorate;   // HEALTH / TECHNICAL
    private String category;

    private String phaseType;     // ROUTINE / PRE_OPENING / etc
    private Integer sortOrder;
    private boolean active;

    private String nextPhaseOnPass;
    private String nextPhaseOnFail;

    // 🔥 ADD THIS (IMPORTANT)
    private boolean supportsOperationalTypes;

    private UUID defaultChecklistId;   // 🔥 default (auto-filled)
    private UUID overrideChecklistId;
}