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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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

        Optional<InspectionRun> existing =
                inspectionRepo.findByTaskIdAndSubmittedAtIsNull(taskId);

        if (existing.isPresent()) {
            // ✅ RETURN EXISTING instead of error
            return toResponse(existing.get());
        }

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

    public InspectionResponse saveAnswers(UUID inspectionId,
                                          List<AnswerItem> answers) {

        InspectionRun run = getRun(inspectionId);

        Map<UUID, InspectionAnswer> existingAnswers =
                run.getAnswers().stream()
                        .collect(Collectors.toMap(
                                InspectionAnswer::getQuestionId,
                                a -> a
                        ));

        for (AnswerItem item : answers) {

            InspectionAnswer existing = existingAnswers.get(item.questionId());

            if (existing != null) {
                // ✅ UPDATE existing
                existing.setAnswer(item.answer());
                existing.setNote(item.note());
            } else {
                // ✅ INSERT new
                run.getAnswers().add(
                        InspectionAnswer.builder()
                                .inspection(run)
                                .questionId(item.questionId())
                                .answer(item.answer())
                                .note(item.note())
                                .build()
                );
            }
        }

        return toResponse(inspectionRepo.save(run));
    }

    // =========================================================
    // SUBMIT INSPECTION
    // =========================================================
    public InspectionResponse submit(UUID inspectionId,
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

        // ── 3. PHASE ADVANCEMENT — same task moves to next phase ──────────────
        // Instead of creating a new task, the existing task's phase field is
        // updated to the next phase and its status reset to PENDING so it
        // re-enters the Work Plans queue for re-assignment and scheduling.
        //
        // If phaseResolverService returns null (e.g. last phase, or FAIL with
        // no next phase configured), the task is marked COMPLETED and no
        // phase advancement happens.
        String nextPhase = phaseResolverService.resolveNextPhase(
                entity.getDirectorate(),
                entity.getCategory(),
                task.getPhase(),
                outcome
        );


        if (nextPhase != null && !nextPhase.isBlank()) {
            PhaseConfig nextPhaseConfig = phaseRepo
                    .findByDirectorateAndCategoryAndPhaseType(
                            entity.getDirectorate(),
                            entity.getCategory(),
                            nextPhase
                    ).orElseThrow();
            // Advance the same task to the next phase
            String previousPhase = task.getPhase();
            task.setPhase(nextPhase);
            task.setStatus("PENDING");           // back to queue — ready for Work Plans
            task.setAssignedTo(null);            // clear assignee: supervisor re-assigns
            if (nextPhaseConfig.getDueDays() != null) {
                task.setDueAt(OffsetDateTime.now().plusDays(nextPhaseConfig.getDueDays()));
            }
            taskRepo.save(task);
            log.info("Task {} advanced: {} → {} for entity {} (status reset to PENDING)",
                    task.getId(), previousPhase, nextPhase, entity.getExternalRef());
        } else {
            // No next phase — this was the final phase or outcome blocks advancement
            task.setStatus("COMPLETED");
            taskRepo.save(task);
            log.info("Task {} completed at final phase {} for entity {}",
                    task.getId(), task.getPhase(), entity.getExternalRef());
        }

        // ── 4. CREATE REINSPECTION TASK (inspector-set date) ─────────────────
        // Explicit inspector action on the Outcome screen (CONDITIONAL / FAIL).
        // This is a separate follow-up task — not a phase progression.
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

        // ── 5. CREATE FOLLOW-UP TASK (Health Ops only) ───────────────────────
        // Explicit inspector action on the Outcome screen.
        // A separate follow-up task — not a phase progression.
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

        // ── 6. WORKFLOW (Flowable — notice generation + Oracle push) ─────────
        // Real fine amount from the Notice already generated on the Outcome screen.
        // fine = 0 for PASS — the BPMN gateway short-circuits to Oracle.
        long fineAmount = noticeRepo
                .findByInspectionIdOrderByCreatedAtDesc(run.getId())
                .stream()
                .filter(n -> n.getFineAmount() != null)
                .mapToLong(n -> n.getFineAmount())
                .sum();

        // Supervisor limit from the inspector's AppUser profile. Default 200.
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

        // ── 7. SAVE + AUDIT ───────────────────────────────────────────────────
        run = inspectionRepo.save(run);
        auditService.log(actor, "SUBMIT_INSPECTION", "InspectionRun", run.getId().toString());

        return toResponse(run);
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
            phaseConfig.setOverrideChecklistName(checklist.getName());
            phaseRepo.save(phaseConfig);
            return checklist;
        }
        if (phaseConfig.getDefaultChecklistId() != null) {
            ChecklistTemplate checklist = checklistService.findById(phaseConfig.getDefaultChecklistId());
            phaseConfig.setDefaultChecklistName(checklist.getName());
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