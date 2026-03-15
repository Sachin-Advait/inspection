package com.gissoft.inspection_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class InspectionDto {

    // ── Start ────────────────────────────────────────────────────────────────

    public record StartRequest(
            @NotNull UUID taskId
    ) {}

    // ── Save answers ─────────────────────────────────────────────────────────

    public record AnswerBatch(
            @NotEmpty List<AnswerItem> answers
    ) {}

    public record AnswerItem(
            @NotNull  UUID   questionId,
            @NotBlank String answer,
            String note
    ) {}

    // ── Submit ───────────────────────────────────────────────────────────────

    public record SubmitRequest(
            String summaryNote
    ) {}

    // ── Response ─────────────────────────────────────────────────────────────

    public record InspectionResponse(
            UUID   id,
            UUID   taskId,
            UUID   entityId,
            String entityName,
            String entityRef,
            UUID   checklistTemplateId,
            int    checklistVersion,
            String startedBy,
            OffsetDateTime startedAt,
            OffsetDateTime submittedAt,
            String outcome,
            String summaryNote,
            List<AnswerItem> answers
    ) {}
}
