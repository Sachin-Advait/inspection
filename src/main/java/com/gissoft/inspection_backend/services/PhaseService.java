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

    public List<PhaseConfig> getPhases(String dg, String category) {
        return repo.findByDirectorateAndCategoryAndActiveTrueOrderBySortOrderAsc(dg, category);
    }

    public PhaseConfig create(PhaseConfig phase) {
        return repo.save(phase);
    }

    public List<PhaseConfig> saveAll(List<PhaseConfig> phases) {
        return repo.saveAll(phases);
    }
}