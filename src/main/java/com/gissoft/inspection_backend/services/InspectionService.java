package com.gissoft.inspection_backend.services;

import com.gissoft.inspection_backend.dto.InspectionDto.AnswerBatch;
import com.gissoft.inspection_backend.dto.InspectionDto.AnswerItem;
import com.gissoft.inspection_backend.dto.InspectionDto.StartRequest;
import com.gissoft.inspection_backend.dto.InspectionDto.SubmitRequest;
import com.gissoft.inspection_backend.entity.*;
import com.gissoft.inspection_backend.repository.EntityMasterRepository;
import com.gissoft.inspection_backend.repository.InspectionRunRepository;
import com.gissoft.inspection_backend.repository.TaskRepository;
import com.gissoft.inspection_backend.workflow.PushOracleService;
import com.gissoft.inspection_backend.workflow.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InspectionService {

    private final InspectionRunRepository inspectionRunRepo;
    private final TaskRepository taskRepo;
    private final EntityMasterRepository entityRepo;
    private final ChecklistService checklistService;
    private final PushOracleService pushOracleService;
    private final WorkflowService workflowService;
    private final AuditService auditService;

    // ── Start ─────────────────────────────────────────────────────────────────

    @Transactional
    public InspectionRun start(StartRequest req, String inspector) {
        Task task = taskRepo.findById(req.taskId())
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + req.taskId()));

        // Guard: one open inspection per task
        inspectionRunRepo.findByTaskIdAndSubmittedAtIsNull(req.taskId()).ifPresent(e -> {
            throw new IllegalStateException(
                    "An open inspection already exists for task: " + req.taskId());
        });

        EntityMaster entity = task.getEntity();
        ChecklistTemplate template = checklistService.getActive(
                entity.getDirectorate(), entity.getCategory(), task.getPhaseOrSubtype());

        task.setStatus("IN_PROGRESS");
        taskRepo.save(task);

        InspectionRun run = InspectionRun.builder()
                .task(task)
                .entity(entity)
                .checklistTemplateId(template.getId())
                .checklistVersion(template.getVersion())
                .startedBy(inspector)
                .startedAt(OffsetDateTime.now())
                .build();

        run = inspectionRunRepo.save(run);
        auditService.log(inspector, "START", "InspectionRun", run.getId().toString());
        return run;
    }

    // ── Save answers (idempotent upsert) ─────────────────────────────────────

    @Transactional
    public InspectionRun saveAnswers(UUID inspectionId, AnswerBatch batch, String inspector) {
        InspectionRun run = getOpenRun(inspectionId, inspector);

        Map<UUID, InspectionAnswer> existing = new HashMap<>();
        run.getAnswers().forEach(a -> existing.put(a.getQuestionId(), a));

        for (AnswerItem item : batch.answers()) {
            InspectionAnswer ans = existing.computeIfAbsent(item.questionId(),
                    qid -> InspectionAnswer.builder()
                            .inspection(run)
                            .questionId(qid)
                            .answer(item.answer())
                            .note(item.note())
                            .build());
            ans.setAnswer(item.answer());
            ans.setNote(item.note());
        }

        run.getAnswers().clear();
        run.getAnswers().addAll(existing.values());
        return inspectionRunRepo.save(run);
    }

    // ── Submit ────────────────────────────────────────────────────────────────

    @Transactional
    public InspectionRun submit(UUID inspectionId, SubmitRequest req, String inspector) {
        InspectionRun run = getOpenRun(inspectionId, inspector);

        String outcome = determineOutcome(run);
        run.setOutcome(outcome);
        run.setSummaryNote(req.summaryNote());
        run.setSubmittedAt(OffsetDateTime.now());

        // Close task
        run.getTask().setStatus("COMPLETED");
        taskRepo.save(run.getTask());

        // Update entity summary
        EntityMaster entity = run.getEntity();
        entity.setLastInspectionAt(run.getSubmittedAt());
        entity.setLastInspectionResult(outcome);
        entityRepo.save(entity);

        // Approval or direct Oracle push
        boolean needsApproval = !"PASS".equals(outcome) &&
                "ORACLE".equals(entity.getSourceSystem());
        if (needsApproval) {
            // Create DB approval record for portal queue
            ApprovalRequest approval = ApprovalRequest.builder()
                    .inspection(run)
                    .requiredLevel("SUPERVISOR")
                    .status("PENDING")
                    .build();
            run.getApprovalRequests().add(approval);

            // Also kick off the Flowable process — handles notice generation
            // and Oracle push as service tasks within the BPMN
            try {
                String noticeType = deriveNoticeType(run);
                workflowService.startInspectionProcess(
                        0,                                       // fine resolved by approval
                        run.getId().toString(),
                        entity.getId().toString(),
                        outcome,
                        noticeType,
                        inspector
                );
            } catch (Exception ex) {
                // Flowable failure must not roll back the inspection submission
                auditService.log(inspector, "WORKFLOW_START_FAILED", "InspectionRun",
                        inspectionId.toString(),
                        Map.of("error", ex.getMessage()), null);
            }

        } else if ("ORACLE".equals(entity.getSourceSystem())) {
            // PASS with Oracle source — direct outbox push, no approval needed
//            pushOracleService.enqueue("RESULT_UPDATE", entity, inspectionId, outcome);
        }

        run = inspectionRunRepo.save(run);
        auditService.log(inspector, "SUBMIT", "InspectionRun", inspectionId.toString(),
                Map.of("outcome", outcome), null);
        return run;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public InspectionRun findById(UUID id) {
        return inspectionRunRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found: " + id));
    }

    public List<InspectionRun> findByEntity(UUID entityId) {
        return inspectionRunRepo.findByEntityIdOrderByStartedAtDesc(entityId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private InspectionRun getOpenRun(UUID id, String inspector) {
        InspectionRun run = findById(id);
        if (run.getSubmittedAt() != null) {
            throw new IllegalStateException("Inspection already submitted: " + id);
        }
        if (!run.getStartedBy().equals(inspector)) {
            throw new SecurityException("Not authorised to modify this inspection");
        }
        return run;
    }

    private String determineOutcome(InspectionRun run) {
        boolean anyFail = run.getAnswers().stream().anyMatch(a ->
                "FAIL".equalsIgnoreCase(a.getAnswer()) ||
                        "NO".equalsIgnoreCase(a.getAnswer()));
        return anyFail ? "FAIL" : "PASS";
    }

    /**
     * Simple heuristic: any CRITICAL answer → CLOSURE, else FINE.
     */
    private String deriveNoticeType(InspectionRun run) {
        boolean hasCritical = run.getAnswers().stream()
                .anyMatch(a -> "FAIL".equalsIgnoreCase(a.getAnswer()) &&
                        a.getNote() != null &&
                        a.getNote().toUpperCase().contains("CRITICAL"));
        return hasCritical ? "CLOSURE" : "FINE";
    }
}
