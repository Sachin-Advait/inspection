package com.gissoft.inspection_backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;

import java.util.UUID;

@Entity
@Data
public class OperationalTypeConfig {

    @Id
    @GeneratedValue
    private UUID id;

    private String directorate;   // HEALTH
    private String category;

    private String phaseType;     // 🔥 LINK TO PHASE

    private String type;          // ROUTINE / RANDOM / COMPLAINT / FOLLOW_UP

    private Integer frequencyDays;
    private Integer dueAfterDays;

    private String defaultChecklist;
    private String priority;

    private boolean active;
}