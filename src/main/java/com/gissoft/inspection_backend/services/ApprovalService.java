package com.gissoft.inspection_backend.services;

import com.gissoft.inspection_backend.dto.ApprovalDto.DecisionRequest;
import com.gissoft.inspection_backend.entity.ApprovalRequest;
import com.gissoft.inspection_backend.repository.ApprovalRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApprovalService {

    private final ApprovalRequestRepository approvalRepo;
    private final AuditService auditService;

    // ── Queue ─────────────────────────────────────────────────────────────────

    public Page<ApprovalRequest> getPending(String level, Pageable pageable) {
        return approvalRepo.findPending(level, pageable);
    }

    // ── Approve ───────────────────────────────────────────────────────────────

    @Transactional
    public ApprovalRequest approve(UUID approvalId, DecisionRequest req, String actor) {
        ApprovalRequest approval = findAndAssertPending(approvalId);

        approval.setStatus("APPROVED");
        approval.setDecisionBy(actor);
        approval.setDecisionNote(req != null ? req.decisionNote() : null);
        approval.setDecidedAt(OffsetDateTime.now());

        var run = approval.getInspection();
        var entity = run.getEntity();

        // Push ENFORCEMENT_UPDATE to Oracle for Oracle-sourced entities
        // Oracle integration removed (demo mode)
        if ("ORACLE".equals(entity.getSourceSystem())) {
            log.info("Oracle push skipped (demo mode) for entity {}", entity.getId());
        }

        approvalRepo.save(approval);
        auditService.log(actor, "APPROVE", "ApprovalRequest", approvalId.toString(),
                Map.of("note", approval.getDecisionNote() != null
                        ? approval.getDecisionNote() : ""), null);
        return approval;
    }

    // ── Reject ────────────────────────────────────────────────────────────────

    @Transactional
    public ApprovalRequest reject(UUID approvalId, DecisionRequest req, String actor) {
        ApprovalRequest approval = findAndAssertPending(approvalId);

        approval.setStatus("REJECTED");
        approval.setDecisionBy(actor);
        approval.setDecisionNote(req != null ? req.decisionNote() : null);
        approval.setDecidedAt(OffsetDateTime.now());

        approvalRepo.save(approval);
        auditService.log(actor, "REJECT", "ApprovalRequest", approvalId.toString());
        return approval;
    }

    // ── Escalate to Manager ───────────────────────────────────────────────────

    @Transactional
    public ApprovalRequest escalate(UUID approvalId, String actor) {
        ApprovalRequest approval = findAndAssertPending(approvalId);

        approval.setRequiredLevel("MANAGER");
        approval.setStatus("PENDING");   // reset so manager can see it
        approvalRepo.save(approval);

        auditService.log(actor, "ESCALATE", "ApprovalRequest", approvalId.toString());
        return approval;
    }

    // ── Request reinspection ──────────────────────────────────────────────────

    @Transactional
    public ApprovalRequest requestReinspection(UUID approvalId, String note, String actor) {
        ApprovalRequest approval = findAndAssertPending(approvalId);

        approval.setStatus("REINSPECTION_REQUESTED");
        approval.setDecisionBy(actor);
        approval.setDecisionNote(note);
        approval.setDecidedAt(OffsetDateTime.now());

        approvalRepo.save(approval);
        auditService.log(actor, "REQUEST_REINSPECTION", "ApprovalRequest", approvalId.toString());
        return approval;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ApprovalRequest findAndAssertPending(UUID id) {
        ApprovalRequest approval = approvalRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Approval not found: " + id));
        if (!"PENDING".equals(approval.getStatus())) {
            throw new IllegalStateException("Approval already decided: " + approval.getStatus());
        }
        return approval;
    }
}
