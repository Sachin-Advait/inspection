package com.gissoft.inspection_backend.controller;

import com.gissoft.inspection_backend.dto.TaskDto;
import com.gissoft.inspection_backend.workflow.TaskQueryService;
import com.gissoft.inspection_backend.workflow.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.flowable.task.api.Task;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/inspection")
public class InspectionController {

    private final WorkflowService workflowService;
    private final TaskQueryService taskQueryService;

    @GetMapping("/tasks/inspector")
    public List<TaskDto> inspectorTasks(){
        return taskQueryService.getInspectorTasks();
    }

    @GetMapping("/tasks/supervisor")
    public List<TaskDto> supervisorTasks(){
        return taskQueryService.getSupervisorTasks();
    }

    @PostMapping("/start")
    public String startInspection(@RequestParam Integer fine){
        return workflowService.startInspection(fine);
    }

    @GetMapping("/tasks")
    public List<TaskDto> getTasks(){
        return taskQueryService.getSupervisorTasks();
    }

    @PostMapping("/complete")
    public void completeTask(@RequestParam String taskId){
        taskQueryService.completeTask(taskId);
    }
}