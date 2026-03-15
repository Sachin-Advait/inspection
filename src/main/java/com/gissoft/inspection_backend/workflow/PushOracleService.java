package com.gissoft.inspection_backend.workflow;

import com.gissoft.inspection_backend.entity.EntityMaster;
import com.gissoft.inspection_backend.repository.EntityMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Demo version (Oracle integration removed)
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class PushOracleService implements JavaDelegate {

    private final EntityMasterRepository entityRepo;

    @Override
    public void execute(DelegateExecution execution) {

        Integer fine = (Integer) execution.getVariable("fine");
        String entityId = (String) execution.getVariable("entityId");
        String inspId = (String) execution.getVariable("inspectionId");
        String outcome = (String) execution.getVariable("outcome");

        log.info("Processing enforcement (Demo Mode)");
        log.info("Fine: {}", fine);
        log.info("EntityId: {}", entityId);
        log.info("InspectionId: {}", inspId);
        log.info("Outcome: {}", outcome);

        if (entityId != null) {
            Optional<EntityMaster> entity =
                    entityRepo.findById(UUID.fromString(entityId));

            entity.ifPresent(e ->
                    log.info("Entity found: {}", e.getExternalRef()));
        }

        log.info("Oracle push skipped (demo mode)");
    }
}