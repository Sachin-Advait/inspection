package com.gissoft.inspection_backend.services;

import com.gissoft.inspection_backend.dto.CreateTaskOnlyRequest;
import com.gissoft.inspection_backend.entity.EntityMaster;
import com.gissoft.inspection_backend.entity.Task;
import com.gissoft.inspection_backend.repository.EntityMasterRepository;
import com.gissoft.inspection_backend.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaskCreationService {

    private final EntityMasterRepository entityRepo;
    private final TaskRepository taskRepo;
    private final AuditService auditService;

    public Task createTaskOnly(CreateTaskOnlyRequest req, String actor) {

        // 1. Create Entity
        EntityMaster entity = EntityMaster.builder()
                .externalRef("INT-" + System.currentTimeMillis())
                .directorate(req.directorate())
                .category(req.category())
                .sourceSystem("INTERNAL")
                .name(req.entityName())
                .ownerName(req.ownerName())
                .ownerPhone(req.ownerPhone())
                .lat(req.lat())
                .lon(req.lon())
                .complianceFlag("ACTIVE")
                .build();

        entity = entityRepo.save(entity);

        // ✅ AUDIT (Entity creation)
        auditService.log(actor, "CREATE", "EntityMaster", entity.getId().toString());

        // 2. Create Task (NO ASSIGNEE)
        Task task = Task.builder()
                .entity(entity)
                .taskType(req.taskType())
                .phase(req.phase())
                .subtype(req.subtype())
                .assignedTo(null)
                .status("PENDING")
                .priority(req.priority() != null ? req.priority() : "MEDIUM")
                .dueAt(req.dueAt())
                .sourceSystem("INTERNAL")
                .build();

        task = taskRepo.save(task);

        // ✅ AUDIT (Task creation)
        auditService.log(actor, "CREATE", "Task", task.getId().toString());

        return task;
    }
}