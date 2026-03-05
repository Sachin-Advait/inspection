package com.gissoft.inspection_backend.workflow;

import lombok.RequiredArgsConstructor;
import org.flowable.engine.RuntimeService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final RuntimeService runtimeService;

    public String startInspection(Integer fine) {

        Map<String,Object> vars = new HashMap<>();
        vars.put("fine", fine);
        vars.put("supervisorLimit", 200);

        return runtimeService.startProcessInstanceByKey(
                "permit_inspection_process_municipality",
                vars
        ).getId();
    }
}