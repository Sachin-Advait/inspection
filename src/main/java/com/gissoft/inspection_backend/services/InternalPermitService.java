package com.gissoft.inspection_backend.services;

import com.gissoft.inspection_backend.dto.PermitDto.CreatePermitRequest;
import com.gissoft.inspection_backend.entity.EntityMaster;
import com.gissoft.inspection_backend.entity.InternalPermit;
import com.gissoft.inspection_backend.entity.PhaseConfig;
import com.gissoft.inspection_backend.entity.Task;
import com.gissoft.inspection_backend.repository.EntityMasterRepository;
import com.gissoft.inspection_backend.repository.InternalPermitRepository;
import com.gissoft.inspection_backend.repository.PhaseConfigRepository;
import com.gissoft.inspection_backend.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InternalPermitService {

    private final InternalPermitRepository permitRepo;
    private final EntityMasterRepository   entityRepo;
    private final TaskRepository           taskRepo;
    private final AuditService             auditService;
    private final PhaseConfigRepository phaseRepo;

    // ── Create permit (DRAFT) ─────────────────────────────────────────────────

    @Transactional
    public InternalPermit create(CreatePermitRequest req, String actor) {
        String permitNo = generatePermitNo(req.category());

        InternalPermit permit = InternalPermit.builder()
                .permitNo(permitNo)
                .category(req.category())
                .contractor(req.contractor())
                .contact(req.contact())
                .area(req.area())
                .lat(req.lat())
                .lon(req.lon())
                .status("DRAFT")
                .createdBy(actor)
                .build();

        permit = permitRepo.save(permit);
        auditService.log(actor, "CREATE", "InternalPermit", permit.getId().toString());
        return permit;
    }

    // ── Activate → creates entity_master + all phase tasks ───────────────────

    @Transactional
    public InternalPermit activate(UUID permitId, String actor) {
        InternalPermit permit = findById(permitId);

        if (!"DRAFT".equals(permit.getStatus())) {
            throw new IllegalStateException("Only DRAFT permits can be activated");
        }

        // Create entity_master for this permit
        EntityMaster entity = EntityMaster.builder()
                .externalRef(permit.getPermitNo())
                .directorate("TECHNICAL")
                .category(permit.getCategory())
                .sourceSystem("INTERNAL")
                .name(permit.getContractor() + " — " + permit.getPermitNo())
                .ownerName(permit.getContractor())
                .ownerPhone(permit.getContact())
                .lat(permit.getLat())
                .lon(permit.getLon())
                .complianceFlag("ACTIVE")
                .build();

        entity = entityRepo.save(entity);

        // Link permit to entity
        permit.setEntityId(entity.getId());
        permit.setStatus("ACTIVE");
        permit.setActivatedAt(OffsetDateTime.now());
        String firstPhase = firstPhase("TECHNICAL", permit.getCategory());

        permit.setCurrentPhase(firstPhase);

        createFirstTask(entity, "TECHNICAL", permit.getCategory());

        permit = permitRepo.save(permit);
        auditService.log(actor, "ACTIVATE", "InternalPermit", permitId.toString());
        return permit;
    }

    // ── Close permit ──────────────────────────────────────────────────────────

    @Transactional
    public InternalPermit close(UUID permitId, String actor) {
        InternalPermit permit = findById(permitId);
        permit.setStatus("CLOSED");
        permit = permitRepo.save(permit);
        auditService.log(actor, "CLOSE", "InternalPermit", permitId.toString());
        return permit;
    }

    // ── List / search ─────────────────────────────────────────────────────────

    public Page<InternalPermit> list(String category, String status,
                                      String area, Pageable pageable) {
        return permitRepo.findByFilters(category, status, area, pageable);
    }

    public InternalPermit findById(UUID id) {
        return permitRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Permit not found: " + id));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String generatePermitNo(String category) {
        String prefix = switch (category.toUpperCase()) {
            case "ROAD"     -> "RD";
            case "LIGHTS"   -> "LGT";
            case "BUILDING" -> "BLD";
            default         -> "INT";
        };
        String year = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy"));
        String seq  = String.format("%06d", System.currentTimeMillis() % 1_000_000);
        return prefix + "-" + year + "-" + seq;
    }


    private String firstPhase(String dg, String category) {
        return phaseRepo
                .findByDirectorateAndCategoryAndActiveTrueOrderBySortOrderAsc(dg, category)
                .stream()
                .findFirst()
                .map(PhaseConfig::getPhaseType)
                .orElseThrow(() -> new IllegalStateException("No phases configured"));
    }

    private void createFirstTask(EntityMaster entity, String dg, String category) {

        String firstPhase = firstPhase(dg, category);

        Task task = Task.builder()
                .entity(entity)
                .taskType("INSPECTION")
                .phase(firstPhase)
                .subtype("INITIAL")
                .assignedTo("")
                .status("PENDING")
                .priority("MEDIUM")
                .sourceSystem("INTERNAL")
                .build();

        taskRepo.save(task);
    }
}
