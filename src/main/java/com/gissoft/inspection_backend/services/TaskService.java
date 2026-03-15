package com.gissoft.inspection_backend.services;

import com.gissoft.inspection_backend.dto.CreateTaskRequest;
import com.gissoft.inspection_backend.dto.TaskFilterRequest;
import com.gissoft.inspection_backend.entity.EntityMaster;
import com.gissoft.inspection_backend.entity.Task;
import com.gissoft.inspection_backend.repository.EntityMasterRepository;
import com.gissoft.inspection_backend.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepo;
    private final EntityMasterRepository entityRepo;
    private final AuditService auditService;

    // ── Mobile: my tasks ──────────────────────────────────────────────────────

    public List<Task> getMyTasks(String username, OffsetDateTime from, OffsetDateTime to) {
        return taskRepo.findMyTasks(username, from, to);
    }

    // ── Ops: paged filtered list ──────────────────────────────────────────────

    public Page<Task> getFiltered(TaskFilterRequest filter, Pageable pageable) {
        return taskRepo.findByFilters(
                filter.getDg(), filter.getStatus(), filter.getAssignedTo(),
                filter.getWorkPlanId(), filter.getPriority(), pageable);
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Transactional
    public Task create(CreateTaskRequest req, String actor) {
        EntityMaster entity = entityRepo.findById(req.getEntityId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Entity not found: " + req.getEntityId()));

        Task task = Task.builder()
                .entity(entity)
                .taskType(req.getTaskType())
                .phaseOrSubtype(req.getPhaseOrSubtype())
                .assignedTo(req.getAssignedTo())
                .status("PENDING")
                .dueAt(req.getDueAt())
                .priority(req.getPriority() != null ? req.getPriority() : "MEDIUM")
                .workPlanId(req.getWorkPlanId())
                .sourceSystem(entity.getSourceSystem())
                .build();

        task = taskRepo.save(task);
        auditService.log(actor, "CREATE", "Task", task.getId().toString());
        return task;
    }

    // ── Reassign ──────────────────────────────────────────────────────────────

    @Transactional
    public Task reassign(UUID taskId, String newAssignee, String actor) {
        Task task = findById(taskId);
        String old = task.getAssignedTo();
        task.setAssignedTo(newAssignee);
        task = taskRepo.save(task);
        auditService.log(actor, "REASSIGN", "Task", taskId.toString(),
                java.util.Map.of("from", old, "to", newAssignee), null);
        return task;
    }

    // ── Reschedule ────────────────────────────────────────────────────────────

    @Transactional
    public Task reschedule(UUID taskId, OffsetDateTime newDue, String actor) {
        Task task = findById(taskId);
        task.setDueAt(newDue);
        task = taskRepo.save(task);
        auditService.log(actor, "RESCHEDULE", "Task", taskId.toString());
        return task;
    }

    // ── Cancel ────────────────────────────────────────────────────────────────

    @Transactional
    public Task cancel(UUID taskId, String actor) {
        Task task = findById(taskId);
        task.setStatus("CANCELLED");
        task = taskRepo.save(task);
        auditService.log(actor, "CANCEL", "Task", taskId.toString());
        return task;
    }

    // ── Lookup ────────────────────────────────────────────────────────────────

    public Task findById(UUID id) {
        return taskRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));
    }

    public List<Task> findByEntity(UUID entityId) {
        return taskRepo.findByEntityIdOrderByDueAtAsc(entityId);
    }
}