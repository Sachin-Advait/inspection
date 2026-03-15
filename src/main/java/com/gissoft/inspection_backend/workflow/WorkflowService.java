package com.gissoft.inspection_backend.workflow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowService {

    private final RuntimeService runtimeService;

    /**
     * Starts the permit inspection BPMN process.
     *
     * @param fine the fine amount — process uses this to route
     *             to supervisor approval if fine > supervisorLimit
     * @return Flowable process instance ID
     */
    public String startInspection(Integer fine) {

        Map<String, Object> vars = new HashMap<>();
        vars.put("fine", fine);
        vars.put("supervisorLimit", 200);

        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                "permit_inspection_process_municipality",
                vars
        );

        log.info("Started process instance: {} for fine: {}", instance.getId(), fine);
        return instance.getId();
    }

    /**
     * Starts the inspection process with full context variables.
     *
     * @param fine          fine amount
     * @param inspectionId  UUID string of the InspectionRun
     * @param entityId      UUID string of the EntityMaster
     * @param outcome       PASS | FAIL | CONDITIONAL
     * @param noticeType    WARNING | FINE | CLOSURE
     * @param generatedBy   username of the submitting inspector
     * @return Flowable process instance ID
     */
    public String startInspectionProcess(Integer fine,
                                          String inspectionId,
                                          String entityId,
                                          String outcome,
                                          String noticeType,
                                          String generatedBy) {

        Map<String, Object> vars = new HashMap<>();
        vars.put("fine",            fine);
        vars.put("supervisorLimit", 200);
        vars.put("inspectionId",    inspectionId);
        vars.put("entityId",        entityId);
        vars.put("outcome",         outcome);
        vars.put("noticeType",      noticeType);
        vars.put("generatedBy",     generatedBy);
        vars.put("eventType",       "ENFORCEMENT_UPDATE");

        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                "permit_inspection_process_municipality",
                vars
        );

        log.info("Started full inspection process: {}, entity: {}, outcome: {}",
                 instance.getId(), entityId, outcome);
        return instance.getId();
    }

    /**
     * Check if a process instance is still active.
     */
    public boolean isRunning(String processInstanceId) {
        return runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .count() > 0;
    }

    /**
     * Set a variable on a running process instance.
     */
    public void setVariable(String processInstanceId, String key, Object value) {
        runtimeService.setVariable(processInstanceId, key, value);
    }

    /**
     * Get a variable from a running process instance.
     */
    public Object getVariable(String processInstanceId, String key) {
        return runtimeService.getVariable(processInstanceId, key);
    }
}
