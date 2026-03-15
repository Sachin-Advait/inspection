package com.gissoft.inspection_backend.controller;

import com.gissoft.inspection_backend.dto.TaskDto;
import com.gissoft.inspection_backend.workflow.TaskQueryService;
import com.gissoft.inspection_backend.workflow.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/workflow")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;
    private final TaskQueryService taskQueryService;

    /**
     * POST /api/workflow/start
     * Starts the permit_inspection_process_municipality process.
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, String>> startWorkflow(
            @RequestBody Map<String, Object> body) {
        Integer fine = body.get("fine") != null
                ? ((Number) body.get("fine")).intValue() : 0;
        String inspectionId = (String) body.get("inspectionId");
        String entityId = (String) body.get("entityId");
        String outcome = (String) body.getOrDefault("outcome", "FAIL");
        String noticeType = (String) body.getOrDefault("noticeType", "WARNING");
        String generatedBy = (String) body.getOrDefault("generatedBy", "system");

        String processId = workflowService.startInspectionProcess(
                fine, inspectionId, entityId, outcome, noticeType, generatedBy);

        return ResponseEntity.ok(Map.of("processInstanceId", processId));
    }

    /**
     * GET /api/workflow/tasks/supervisor
     */
    @GetMapping("/tasks/supervisor")
    public ResponseEntity<List<TaskDto>> getSupervisorTasks() {
        return ResponseEntity.ok(taskQueryService.getSupervisorTasks());
    }

    /**
     * GET /api/workflow/tasks/inspector
     */
    @GetMapping("/tasks/inspector")
    public ResponseEntity<List<TaskDto>> getInspectorTasks() {
        return ResponseEntity.ok(taskQueryService.getInspectorTasks());
    }

    /**
     * POST /api/workflow/tasks/{taskId}/complete
     */
    @PostMapping("/tasks/{taskId}/complete")
    public ResponseEntity<Map<String, String>> completeWorkflowTask(
            @PathVariable String taskId,
            @RequestBody(required = false) Map<String, Object> variables) {
        if (variables != null && !variables.isEmpty()) {
            taskQueryService.completeTask(taskId, variables);
        } else {
            taskQueryService.completeTask(taskId);
        }
        return ResponseEntity.ok(Map.of("status", "completed", "taskId", taskId));
    }
}