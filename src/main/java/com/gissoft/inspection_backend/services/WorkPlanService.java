package com.gissoft.inspection_backend.services;

import com.gissoft.inspection_backend.dto.WorkPlanDto.CreatePlanRequest;
import com.gissoft.inspection_backend.dto.WorkPlanDto.TaskAssignment;
import com.gissoft.inspection_backend.entity.WorkPlan;
import com.gissoft.inspection_backend.repository.TaskRepository;
import com.gissoft.inspection_backend.repository.WorkPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkPlanService {

    private final WorkPlanRepository workPlanRepo;
    private final TaskRepository     taskRepo;
    private final AuditService       auditService;

    @Transactional
    public WorkPlan createPlan(CreatePlanRequest req, String actor) {
        WorkPlan plan = WorkPlan.builder()
                .planType(req.planType())
                .dateFrom(req.dateFrom())
                .dateTo(req.dateTo())
                .dg(req.dg())
                .category(req.category())
                .area(req.area())
                .status("DRAFT")
                .createdBy(actor)
                .build();

        plan = workPlanRepo.save(plan);

        if (req.assignments() != null) {
            for (TaskAssignment a : req.assignments()) {
                applyAssignment(plan.getId(), a, actor); // ✅ pass actor
            }
        }

        auditService.log(actor, "CREATE", "WorkPlan", plan.getId().toString());
        return plan;
    }

    @Transactional
    public WorkPlan publish(UUID planId, String actor) {
        WorkPlan plan = findById(planId);

        if (!"DRAFT".equals(plan.getStatus())) {
            throw new IllegalStateException("Only DRAFT plans can be published");
        }

        plan.setStatus("PUBLISHED");
        plan.setPublishedAt(OffsetDateTime.now());
        plan = workPlanRepo.save(plan);

        auditService.log(actor, "PUBLISH", "WorkPlan", planId.toString());
        log.info("Work plan {} published by {}", planId, actor);
        return plan;
    }

    public Map<String, List<UUID>> autoDistribute(List<UUID> taskIds,
                                                  List<String> inspectors) {
        if (inspectors == null || inspectors.isEmpty()) {
            throw new IllegalArgumentException("Inspector list must not be empty");
        }

        Map<String, List<UUID>> dist = new LinkedHashMap<>();
        for (String i : inspectors) dist.put(i, new ArrayList<>());

        int idx = 0;
        for (UUID taskId : taskIds) {
            String inspector = inspectors.get(idx % inspectors.size());
            dist.get(inspector).add(taskId);
            idx++;
        }
        return dist;
    }

    public List<WorkPlan> findByStatus(String status) {
        return workPlanRepo.findByStatusOrderByDateFromDesc(status);
    }

    public WorkPlan findById(UUID id) {
        return workPlanRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Work plan not found: " + id));
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private void applyAssignment(UUID planId, TaskAssignment a, String actor) {
        taskRepo.findById(a.taskId()).ifPresentOrElse(task -> {

            String oldAssignee = task.getAssignedTo();

            task.setAssignedTo(a.assignedTo());
            if (a.dueAt() != null) task.setDueAt(a.dueAt());
            task.setWorkPlanId(planId);

            taskRepo.save(task);

            // ✅ AUDIT (assignment change)
            Map<String, Object> changes = new HashMap<>();
            changes.put("from", oldAssignee);
            changes.put("to", a.assignedTo());
            changes.put("workPlanId", planId.toString());

            auditService.log(actor, "ASSIGN_TASK", "Task", task.getId().toString(), changes);

        }, () -> log.warn("Task not found during assignment: {}", a.taskId()));
    }
}