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

    private String directorate;
    private String category;
    private String phaseType;

    private String type;

    private Integer frequencyDays;
    private Integer dueAfterDays;

    private String priority;
    private boolean active;

    // ✅ ADD THIS
    private UUID checklistTemplateId;
}