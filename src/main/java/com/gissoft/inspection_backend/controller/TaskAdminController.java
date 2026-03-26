package com.gissoft.inspection_backend.controller;

import com.gissoft.inspection_backend.dto.CreateTaskOnlyRequest;
import com.gissoft.inspection_backend.entity.Task;
import com.gissoft.inspection_backend.services.TaskCreationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/admin/tasks")
@RequiredArgsConstructor
public class TaskAdminController {

    private final TaskCreationService service;

    @PostMapping("/create")
    public Task createTask(@RequestBody CreateTaskOnlyRequest req,
                           Principal principal) {
        return service.createTaskOnly(req, principal.getName());
    }
}