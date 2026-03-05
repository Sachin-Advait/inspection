package com.gissoft.inspection_backend.workflow;

import com.gissoft.inspection_backend.dto.TaskDto;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskQueryService {

    private final TaskService taskService;

    public List<TaskDto> getSupervisorTasks(){

        List<Task> tasks = taskService.createTaskQuery()
                .taskCandidateGroup("supervisor")
                .list();

        return tasks.stream()
                .map(t -> new TaskDto(
                        t.getId(),
                        t.getName(),
                        t.getAssignee(),
                        t.getProcessInstanceId()
                ))
                .collect(Collectors.toList());
    }
    public List<TaskDto> getInspectorTasks(){

        List<Task> tasks = taskService.createTaskQuery()
                .taskCandidateGroup("inspector")
                .list();

        return tasks.stream()
                .map(t -> new TaskDto(
                        t.getId(),
                        t.getName(),
                        t.getAssignee(),
                        t.getProcessInstanceId()
                ))
                .collect(Collectors.toList());
    }

    public void completeTask(String taskId){
        taskService.complete(taskId);
    }
}