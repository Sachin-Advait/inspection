package com.gissoft.inspection_backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ApprovalDto {

    public record ApprovalResponse(
            UUID   id,
            UUID   inspectionId,
            String entityRef,
            String entityName,
            String outcome,
            String requiredLevel,
            String status,
            String decisionBy,
            String decisionNote,
            OffsetDateTime decidedAt,
            OffsetDateTime createdAt
    ) {}

    public record DecisionRequest(
            String decisionNote
    ) {}
}
