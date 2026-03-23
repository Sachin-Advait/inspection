package com.gissoft.inspection_backend.services;

import com.gissoft.inspection_backend.dto.InspectionDto.AnswerItem;
import com.gissoft.inspection_backend.dto.InspectionDto.InspectionResponse;
import com.gissoft.inspection_backend.dto.InspectionDto.SubmitRequest;
import com.gissoft.inspection_backend.entity.*;
import com.gissoft.inspection_backend.repository.*;
import com.gissoft.inspection_backend.workflow.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class InspectionService {

    private final InspectionRunRepository inspectionRepo;
    private final TaskRepository taskRepo;
    private final ChecklistService checklistService;
    private final PhaseResolverService phaseResolverService;
    private final PhaseConfigRepository phaseRepo;
    private final WorkflowService workflowService;
    private final AuditService auditService;
    private final AppUserRepository userRepo;
    private final NoticeRepository noticeRepo;

    // =========================================================
    // START INSPECTION
    // =========================================================
    public InspectionResponse start(UUID taskId, String actor) {

        Task task = taskRepo.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        inspectionRepo.findByTaskIdAndSubmittedAtIsNull(taskId)
                .ifPresent(i -> {
                    throw new IllegalStateException("Inspection already in progress");
                });

        ChecklistTemplate checklist = resolveChecklist(task);

        InspectionRun run = InspectionRun.builder()
                .task(task)
                .entity(task.getEntity())
                .checklistTemplateId(checklist.getId())
                .checklistVersion(checklist.getVersion())
                .startedBy(actor)
                .startedAt(OffsetDateTime.now())
                .build();

        task.setStatus("IN_PROGRESS");
        taskRepo.save(task);

        return toResponse(inspectionRepo.save(run));
    }

    // =========================================================
    // SAVE ANSWERS
    // =========================================================
    public InspectionResponse saveAnswers(UUID inspectionId,
                                          List<AnswerItem> answers) {

        InspectionRun run = getRun(inspectionId);

        run.getAnswers().clear();

        for (AnswerItem item : answers) {
            run.getAnswers().add(InspectionAnswer.builder()
                    .inspection(run)
                    .questionId(item.questionId())
                    .answer(item.answer())
                    .note(item.note())
                    .build());
        }

        return toResponse(inspectionRepo.save(run));
    }

    // =========================================================
    // SUBMIT INSPECTION
    // =========================================================
    public InspectionRun submit(UUID inspectionId,
                                SubmitRequest req,
                                String actor) {

        InspectionRun run = getRun(inspectionId);

        if (run.getSubmittedAt() != null) {
            throw new IllegalStateException("Already submitted");
        }

        Task task = run.getTask();
        EntityMaster entity = run.getEntity();

        // ── 1. OUTCOME ────────────────────────────────────────────────────────
        // Inspector-confirmed outcome from Flutter takes precedence.
        // Fall back to computing from fail count only if not provided.
        String outcome;
        if (req.outcome() != null && !req.outcome().isBlank()) {
            outcome = req.outcome().toUpperCase();
        } else {
            int failCount = (int) run.getAnswers().stream()
                    .filter(a -> {
                        ChecklistQuestion q = checklistService.getQuestion(a.getQuestionId());
                        return q.getRule() != null && "FAIL".equalsIgnoreCase(a.getAnswer());
                    })
                    .count();

            if (failCount == 0) outcome = "PASS";
            else if (failCount <= 2) outcome = "CONDITIONAL";
            else outcome = "FAIL";
        }

        run.setOutcome(outcome);
        run.setSubmittedAt(OffsetDateTime.now());
        run.setSummaryNote(req.summaryNote());

        // ── 2. ENTITY UPDATE ──────────────────────────────────────────────────
        entity.setLastInspectionAt(run.getSubmittedAt());
        entity.setLastInspectionResult(outcome);

        // Health Operational: update next scheduled due date
        if (req.nextDueDate() != null) {
            entity.setNextDueAt(req.nextDueDate());
            log.info("Next due updated for entity {}: {}", entity.getExternalRef(), req.nextDueDate());
        }

        // ── 3. NEXT PHASE (automatic — phase resolver handles Technical + Licensing) ──
        // Phase resolver determines the next structured phase (e.g. Foundation → Structural).
        // This runs for Technical and Health Licensing flows.
        // For reinspections set by inspector date, we create the task below instead.
        String nextPhase = phaseResolverService.resolveNextPhase(
                entity.getDirectorate(),
                entity.getCategory(),
                task.getPhase(),
                outcome
        );

        // ── 4. CREATE NEXT STRUCTURED PHASE TASK (if resolver found one) ─────
        if (nextPhase != null && !nextPhase.isBlank()) {
            Task nextTask = Task.builder()
                    .entity(entity)
                    .taskType(task.getTaskType())
                    .subtype(task.getSubtype())
                    .phase(nextPhase)
                    .assignedTo(task.getAssignedTo())
                    .status("PENDING")
                    .priority(task.getPriority())
                    .sourceSystem(task.getSourceSystem())
                    .dueAt(task.getDueAt())   // SLA carried from phase config
                    .build();
            taskRepo.save(nextTask);
            log.info("Next phase task created: {} → {} for entity {}",
                    task.getPhase(), nextPhase, entity.getExternalRef());
        }

        // ── 5. CREATE REINSPECTION TASK (inspector-set date, CONDITIONAL/FAIL) ──
        // This is separate from the phase resolver — it's a manual follow-up
        // date the inspector sets on the Outcome screen.
        if (req.reinspectDate() != null) {
            Task reinspectTask = Task.builder()
                    .entity(entity)
                    .taskType("REINSPECTION")
                    .phase("FollowUp")
                    .assignedTo(task.getAssignedTo())
                    .status("PENDING")
                    .priority("HIGH")
                    .sourceSystem("INTERNAL")
                    .dueAt(req.reinspectDate())
                    .build();
            taskRepo.save(reinspectTask);
            log.info("Reinspection task created for entity {} due {}",
                    entity.getExternalRef(), req.reinspectDate());
        }

        // ── 6. CREATE FOLLOW-UP TASK (Health Ops only) ───────────────────────
        if (req.followUpDate() != null) {
            Task followUpTask = Task.builder()
                    .entity(entity)
                    .taskType("REINSPECTION")
                    .phase("FollowUpHygiene")
                    .assignedTo(task.getAssignedTo())
                    .status("PENDING")
                    .priority("HIGH")
                    .sourceSystem("INTERNAL")
                    .dueAt(req.followUpDate())
                    .build();
            taskRepo.save(followUpTask);
            log.info("Follow-up hygiene task created for entity {} due {}",
                    entity.getExternalRef(), req.followUpDate());
        }

        // ── 7. COMPLETE CURRENT TASK ──────────────────────────────────────────
        task.setStatus("COMPLETED");
        taskRepo.save(task);

        // ── 8. WORKFLOW (Flowable — notice generation + Oracle push) ─────────

        // Real fine amount: read from the Notice already generated by the
        // inspector on the Outcome screen (before Submit).
        // If no notice was generated yet (PASS or inspector skipped it),
        // fine = 0 — the BPMN outcome gateway will short-circuit to Oracle.
        long fineAmount = noticeRepo
                .findByInspectionIdOrderByCreatedAtDesc(run.getId())
                .stream()
                .filter(n -> n.getFineAmount() != null)
                .mapToLong(n -> n.getFineAmount())
                .sum();

        // Real supervisor limit: from the inspector's AppUser profile.
        // Admin sets this per user in the portal. Default 200 if not configured.
        long supervisorLimit = userRepo.findByUsername(actor)
                .map(u -> u.getSupervisorFineLimit() != null
                        ? u.getSupervisorFineLimit()
                        : 200L)
                .orElse(200L);

        String noticeType = "FAIL".equals(outcome) ? "FINE" : "WARNING";

        workflowService.startInspectionProcess(
                fineAmount,
                run.getId().toString(),
                entity.getId().toString(),
                outcome,
                noticeType,
                actor,
                supervisorLimit
        );

        // ── 9. SAVE + AUDIT ───────────────────────────────────────────────────
        run = inspectionRepo.save(run);
        auditService.log(actor, "SUBMIT_INSPECTION", "InspectionRun", run.getId().toString());

        return run;
    }

    // =========================================================
    // CHECKLIST RESOLUTION
    // =========================================================
    private ChecklistTemplate resolveChecklist(Task task) {

        String dg = task.getEntity().getDirectorate();
        String category = task.getEntity().getCategory();
        String phase = task.getPhase();

        PhaseConfig phaseConfig = phaseRepo
                .findByDirectorateAndCategoryAndPhaseType(dg, category, phase)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Phase config not found: " + dg + "/" + category + "/" + phase));

        if (phaseConfig.getOverrideChecklistId() != null) {
            ChecklistTemplate checklist = checklistService.findById(phaseConfig.getOverrideChecklistId());
            phaseConfig.setOverrideChecklistName(checklist.getName());  // ← sync
            phaseRepo.save(phaseConfig);
            return checklist;
        }
        if (phaseConfig.getDefaultChecklistId() != null) {
            ChecklistTemplate checklist = checklistService.findById(phaseConfig.getDefaultChecklistId());
            phaseConfig.setDefaultChecklistName(checklist.getName());   // ← sync
            phaseRepo.save(phaseConfig);
            return checklist;
        }

        return checklistService.getActive(dg, category, phase);
    }
    // =========================================================
    // HELPERS
    // =========================================================
    private InspectionRun getRun(UUID id) {
        return inspectionRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found: " + id));
    }

    public List<InspectionRun> findByEntity(UUID entityId) {
        return inspectionRepo.findByEntityIdOrderByStartedAtDesc(entityId);
    }

    private InspectionResponse toResponse(InspectionRun run) {
        return new InspectionResponse(
                run.getId(),
                run.getTask().getId(),
                run.getEntity().getId(),
                run.getEntity().getName(),
                run.getEntity().getExternalRef(),
                run.getChecklistTemplateId(),
                run.getChecklistVersion(),
                run.getStartedBy(),
                run.getStartedAt(),
                run.getSubmittedAt(),
                run.getOutcome(),
                run.getSummaryNote(),
                run.getAnswers().stream()
                        .map(a -> new AnswerItem(a.getQuestionId(), a.getAnswer(), a.getNote()))
                        .toList()
        );
    }
}