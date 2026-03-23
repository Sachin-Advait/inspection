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
     * Start the inspection BPMN process with full context.
     * <p>
     * Variables injected into the process:
     * <p>
     * fine             — actual fine amount from the Notice already generated
     * (0 for PASS, real value from Notice.fineAmount for FAIL/CONDITIONAL)
     * supervisorLimit  — AppUser.supervisorFineLimit of the inspector's org
     * Gateway: fine <= supervisorLimit  → Supervisor approves
     * fine >  supervisorLimit  → Manager approves
     * outcome          — PASS | CONDITIONAL | FAIL
     * Gateway: PASS + fine == 0         → skips notice + approval
     * CONDITIONAL/FAIL         → full enforcement path
     * inspectionId     — UUID for GenerateNoticeService / PushOracleService
     * entityId         — UUID for GenerateNoticeService / PushOracleService
     * noticeType       — WARNING | FINE | CLOSURE
     * generatedBy      — inspector username
     * eventType        — RESULT_UPDATE (PASS) or ENFORCEMENT_UPDATE (FAIL/CONDITIONAL)
     * PushOracleService reads this to pick the right Oracle operation
     */
    public String startInspectionProcess(long fine,
                                         String inspectionId,
                                         String entityId,
                                         String outcome,
                                         String noticeType,
                                         String generatedBy,
                                         long supervisorLimit) {

        Map<String, Object> vars = new HashMap<>();
        vars.put("fine", fine);
        vars.put("supervisorLimit", supervisorLimit);
        vars.put("inspectionId", inspectionId);
        vars.put("entityId", entityId);
        vars.put("outcome", outcome);
        vars.put("noticeType", noticeType);
        vars.put("generatedBy", generatedBy);
        // PASS → Oracle gets RESULT_UPDATE only (no enforcement data)
        // CONDITIONAL/FAIL → Oracle gets ENFORCEMENT_UPDATE after approval
        vars.put("eventType", "PASS".equals(outcome) ? "RESULT_UPDATE" : "ENFORCEMENT_UPDATE");

        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                "permit_inspection_process_municipality",
                vars
        );

        log.info("Started inspection process {} — outcome: {}, fine: {}, supervisorLimit: {}",
                instance.getId(), outcome, fine, supervisorLimit);

        return instance.getId();
    }

    public boolean isRunning(String processInstanceId) {
        return runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .count() > 0;
    }

    public void setVariable(String processInstanceId, String key, Object value) {
        runtimeService.setVariable(processInstanceId, key, value);
    }

    public Object getVariable(String processInstanceId, String key) {
        return runtimeService.getVariable(processInstanceId, key);
    }
}