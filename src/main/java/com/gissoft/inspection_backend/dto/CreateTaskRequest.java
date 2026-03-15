package com.gissoft.inspection_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTaskRequest {

    @NotNull
    private UUID entityId;

    @NotBlank
    private String taskType;

    @NotBlank
    private String phaseOrSubtype;

    @NotBlank
    private String assignedTo;

    private OffsetDateTime dueAt;
    private String priority;
    private UUID workPlanId;
}
