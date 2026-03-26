package com.gissoft.inspection_backend.services;

import com.gissoft.inspection_backend.entity.PhaseConfig;
import com.gissoft.inspection_backend.repository.PhaseConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PhaseService {

    private final PhaseConfigRepository repo;
    private final AuditService auditService;

    public List<PhaseConfig> getPhases(String dg, String category) {
        return repo.findByDirectorateAndCategoryAndActiveTrueOrderBySortOrderAsc(dg, category);
    }

    public PhaseConfig create(PhaseConfig phase, String actor) {
        PhaseConfig saved = repo.save(phase);

        // ✅ AUDIT
        auditService.log(actor, "CREATE", "PhaseConfig", saved.getId().toString());

        return saved;
    }

    public List<PhaseConfig> saveAll(List<PhaseConfig> phases, String actor) {
        List<PhaseConfig> saved = repo.saveAll(phases);

        // ✅ AUDIT
        auditService.log(actor, "UPSERT", "PhaseConfig", "BULK");

        return saved;
    }
}