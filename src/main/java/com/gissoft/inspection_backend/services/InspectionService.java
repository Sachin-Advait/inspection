package com.gissoft.inspection_backend.services;

import com.gissoft.inspection_backend.dto.InspectionDto;
import com.gissoft.inspection_backend.entity.*;
import com.gissoft.inspection_backend.repository.*;
import com.gissoft.inspection_backend.workflow.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class InspectionService {

    private final InspectionRunRepository inspectionRepo;
    private final TaskRepository taskRepo;
    private final ChecklistService checklistService;
    private final PhaseResolverService phaseResolverService;
    private final WorkflowService workflowService;
    private final AuditService auditService;

    // =========================================================
    // START INSPECTION
    // =========================================================
    public InspectionDto.InspectionResponse start(UUID taskId, String actor) {

        Task task = taskRepo.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        inspectionRepo.findByTaskIdAndSubmittedAtIsNull(taskId)
                .ifPresent(i -> {
                    throw new IllegalStateException("Inspection already in progress");
                });

        ChecklistTemplate checklist = checklistService.getActive(
                task.getEntity().getDirectorate(),
                task.getEntity().getCategory(),
                task.getPhase()
        );

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
    public InspectionDto.InspectionResponse saveAnswers(
            UUID inspectionId,
            List<InspectionDto.AnswerItem> answers
    ) {

        InspectionRun run = getRun(inspectionId);

        run.getAnswers().clear();

        for (InspectionDto.AnswerItem item : answers) {

            InspectionAnswer ans = InspectionAnswer.builder()
                    .inspection(run)
                    .questionId(item.questionId())
                    .answer(item.answer())
                    .note(item.note())
                    .build();

            run.getAnswers().add(ans);
        }

        return toResponse(inspectionRepo.save(run));
    }

    // =========================================================
    // SUBMIT INSPECTION (CORE ENGINE)
    // =========================================================
    public InspectionDto.InspectionResponse submit(
            UUID inspectionId,
            String actor,
            String summaryNote
    ) {

        InspectionRun run = getRun(inspectionId);

        if (run.getSubmittedAt() != null) {
            throw new IllegalStateException("Already submitted");
        }

        Task task = run.getTask();
        EntityMaster entity = run.getEntity();

        // ───────────── 1. OUTCOME ─────────────

        int failCount = 0;

        for (InspectionAnswer ans : run.getAnswers()) {

            ChecklistQuestion q = checklistService.getQuestion(ans.getQuestionId());

            if (q.getRule() != null && "FAIL".equalsIgnoreCase(ans.getAnswer())) {
                failCount++;
            }
        }

        String outcome;
        if (failCount == 0) {
            outcome = "PASS";
        } else if (failCount <= 2) {
            outcome = "CONDITIONAL";
        } else {
            outcome = "FAIL";
        }

        run.setOutcome(outcome);
        run.setSubmittedAt(OffsetDateTime.now());
        run.setSummaryNote(summaryNote);

        // ───────────── 2. ENTITY UPDATE ─────────────

        entity.setLastInspectionAt(run.getSubmittedAt());
        entity.setLastInspectionResult(outcome);

        // ───────────── 3. PHASE PROGRESSION ─────────────

        String nextPhase = phaseResolverService.resolveNextPhase(
                entity.getDirectorate(),
                entity.getCategory(),
                task.getPhase(),
                outcome
        );

        // ───────────── 4. NEXT TASK ─────────────

        if (nextPhase != null && !nextPhase.isBlank()) {

            Task newTask = Task.builder()
                    .entity(entity)
                    .taskType("REINSPECTION")
                    .phase(nextPhase)
                    .subtype("AUTO")
                    .assignedTo("") // TODO: assign properly
                    .status("PENDING")
                    .priority("MEDIUM")
                    .sourceSystem(entity.getSourceSystem())
                    .build();

            taskRepo.save(newTask);
        }

        // ───────────── 5. COMPLETE TASK ─────────────

        task.setStatus("COMPLETED");
        taskRepo.save(task);

        // ───────────── 6. WORKFLOW ─────────────

        int fine = failCount * 50;

        String noticeType = switch (outcome) {
            case "FAIL" -> "FINE";
            case "CONDITIONAL" -> "WARNING";
            default -> "WARNING";
        };

        workflowService.startInspectionProcess(
                fine,
                run.getId().toString(),
                entity.getId().toString(),
                outcome,
                noticeType,
                actor
        );

        // ───────────── 7. SAVE + AUDIT ─────────────

        run = inspectionRepo.save(run);

        auditService.log(actor, "SUBMIT_INSPECTION",
                "InspectionRun", run.getId().toString());

        return toResponse(run);
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private InspectionRun getRun(UUID id) {
        return inspectionRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found"));
    }

    public List<InspectionRun> findByEntity(UUID entityId) {
        return inspectionRepo.findByEntityIdOrderByStartedAtDesc(entityId);
    }

    private InspectionDto.InspectionResponse toResponse(InspectionRun run) {

        return new InspectionDto.InspectionResponse(
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
                        .map(a -> new InspectionDto.AnswerItem(
                                a.getQuestionId(),
                                a.getAnswer(),
                                a.getNote()
                        ))
                        .toList()
        );
    }
}