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
    private final NoticeRepository noticeRepo;
    private final AppUserRepository userRepo;

    // ── START ────────────────────────────────────────────────────────────────

    public InspectionResponse start(UUID taskId, String actor) {

        Task task = taskRepo.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        Optional<InspectionRun> existing =
                inspectionRepo.findByTaskIdAndSubmittedAtIsNull(taskId);

        if (existing.isPresent()) {
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

    public InspectionResponse saveAnswers(UUID inspectionId, List<AnswerItem> answers) {

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
                existing.setAnswer(item.answer());
                existing.setNote(item.note());
            } else {
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

    // ── SUBMIT ───────────────────────────────────────────────────────────────

    public InspectionResponse submit(UUID inspectionId, SubmitRequest req, String actor) {

        InspectionRun run = getRun(inspectionId);

        if (run.getSubmittedAt() != null) {
            throw new IllegalStateException("Already submitted");
        }

        Task task = run.getTask();
        EntityMaster entity = run.getEntity();

        String outcome = (req.outcome() != null && !req.outcome().isBlank())
                ? req.outcome().toUpperCase()
                : computeOutcome(run);

        run.setOutcome(outcome);
        run.setSubmittedAt(OffsetDateTime.now());
        run.setSummaryNote(req.summaryNote());

        entity.setLastInspectionAt(run.getSubmittedAt());
        entity.setLastInspectionResult(outcome);

        if (req.nextDueDate() != null) {
            entity.setNextDueAt(req.nextDueDate());
        }

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

            task.setPhase(nextPhase);
            task.setStatus("PENDING");
            task.setAssignedTo(null);

            if (nextPhaseConfig.getDueDays() != null) {
                task.setDueAt(OffsetDateTime.now().plusDays(nextPhaseConfig.getDueDays()));
            }

            taskRepo.save(task);
        } else {
            task.setStatus("COMPLETED");
            taskRepo.save(task);
        }

        if (req.reinspectDate() != null) {
            taskRepo.save(Task.builder()
                    .entity(entity)
                    .taskType("REINSPECTION")
                    .subtype("REINSPECTION")
                    .phase("FollowUp")
                    .status("PENDING")
                    .priority("HIGH")
                    .sourceSystem("INTERNAL")
                    .dueAt(req.reinspectDate())
                    .build());
        }

        if (req.followUpDate() != null) {
            taskRepo.save(Task.builder()
                    .entity(entity)
                    .taskType("REINSPECTION")
                    .subtype("REINSPECTION")
                    .phase("FollowUpHygiene")
                    .status("PENDING")
                    .priority("HIGH")
                    .sourceSystem("INTERNAL")
                    .dueAt(req.followUpDate())
                    .build());
        }

        long fineAmount = noticeRepo
                .findByInspectionIdOrderByCreatedAtDesc(run.getId())
                .stream()
                .filter(n -> n.getFineAmount() != null)
                .mapToLong(Notice::getFineAmount)
                .sum();

        long supervisorLimit = userRepo.findByUsername(actor).filter(u -> u.getSupervisorFineLimit() != null)
                .map(AppUser::getSupervisorFineLimit).orElse(200L);

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

        run = inspectionRepo.save(run);

        // ✅ CLEAN AUDIT
        auditService.log(
                
                actor,
                "SUBMIT_INSPECTION",
                "InspectionRun",
                run.getId().toString()
        );

        return toResponse(run);
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────

    private String computeOutcome(InspectionRun run) {
        int failCount = (int) run.getAnswers().stream()
                .filter(a -> {
                    ChecklistQuestion q = checklistService.getQuestion(a.getQuestionId());
                    return q.getRule() != null && "FAIL".equalsIgnoreCase(a.getAnswer());
                })
                .count();

        if (failCount == 0) return "PASS";
        if (failCount <= 2) return "CONDITIONAL";
        return "FAIL";
    }

    private ChecklistTemplate resolveChecklist(Task task) {

        String dg = task.getEntity().getDirectorate();
        String category = task.getEntity().getCategory();
        String phase = task.getPhase();

        PhaseConfig phaseConfig = phaseRepo
                .findByDirectorateAndCategoryAndPhaseType(dg, category, phase)
                .orElseThrow();

        if (phaseConfig.getOverrideChecklistId() != null) {
            return checklistService.findById(phaseConfig.getOverrideChecklistId());
        }

        if (phaseConfig.getDefaultChecklistId() != null) {
            return checklistService.findById(phaseConfig.getDefaultChecklistId());
        }

        return checklistService.getActive(dg, category, phase);
    }

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