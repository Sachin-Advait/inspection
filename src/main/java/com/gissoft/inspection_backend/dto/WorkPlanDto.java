package com.gissoft.inspection_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class WorkPlanDto {

    public record CreatePlanRequest(
            @NotBlank String planType,
            @NotNull  OffsetDateTime dateFrom,
            @NotNull  OffsetDateTime dateTo,
            @NotBlank String dg,
            String category,
            String area,
            List<TaskAssignment> assignments
    ) {}

    public record TaskAssignment(
            @NotNull UUID   taskId,
            @NotBlank String assignedTo,
            OffsetDateTime dueAt
    ) {}

    public record WorkPlanResponse(
            UUID   id,
            String planType,
            OffsetDateTime dateFrom,
            OffsetDateTime dateTo,
            String dg,
            String category,
            String area,
            String status,
            OffsetDateTime publishedAt,
            String createdBy,
            OffsetDateTime createdAt
    ) {}
}
