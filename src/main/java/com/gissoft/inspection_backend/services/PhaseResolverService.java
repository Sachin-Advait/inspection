package com.gissoft.inspection_backend.services;

import com.gissoft.inspection_backend.entity.PhaseConfig;
import com.gissoft.inspection_backend.repository.PhaseConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PhaseResolverService {

    private final PhaseConfigRepository repo;

    public String resolveNextPhase(String dg, String category,
                                   String currentPhase, String outcome) {

        PhaseConfig phase = repo
                .findByDirectorateAndCategoryAndPhaseType(dg, category, currentPhase)
                .orElseThrow(() -> new IllegalArgumentException("Phase not found"));

        if ("PASS".equalsIgnoreCase(outcome)) {
            return phase.getNextPhaseOnPass();
        }

        if ("FAIL".equalsIgnoreCase(outcome)) {
            return phase.getNextPhaseOnFail();
        }

        // ✅ ADD THIS (your system already produces CONDITIONAL)
        if ("CONDITIONAL".equalsIgnoreCase(outcome)) {
            return phase.getNextPhaseOnFail(); // usually reinspection
        }

        return null;
    }
}