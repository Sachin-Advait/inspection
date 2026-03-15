package com.gissoft.inspection_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public class ChecklistDto {

    public record CreateTemplateRequest(
            @NotBlank String dg,
            @NotBlank String category,
            @NotBlank String phaseType
    ) {}

    public record AddSectionRequest(
            int    sortOrder,
            @NotBlank String title,
            String description
    ) {}

    public record AddQuestionRequest(
            int    sortOrder,
            @NotBlank String text,
            @NotBlank String answerType,    // PASS_FAIL | YES_NO | TEXT | NUMBER | CHOICE
            boolean required,
            Map<String, Object> validationsJson
    ) {}

    public record SetRuleRequest(
            @NotBlank String failSeverity,          // MINOR | MAJOR | CRITICAL
            Map<String, Object> evidencePolicyJson,
            String violationCode,
            String defaultAction,                   // WARNING | FINE | CLOSURE
            String forceApprovalLevel,              // SUPERVISOR | MANAGER
            Map<String, Object> reinspectionSuggestionJson
    ) {}

    public record PublishRequest(
            String releaseNotes
    ) {}
}
