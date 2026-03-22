package com.gissoft.inspection_backend.repository;

import com.gissoft.inspection_backend.entity.OperationalTypeConfig;
import com.gissoft.inspection_backend.entity.PhaseConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PhaseConfigRepository extends JpaRepository<PhaseConfig, UUID> {

    Optional<PhaseConfig> findByDirectorateAndCategoryAndPhaseType(
            String dg, String category, String phaseType
    );

    List<PhaseConfig> findByDirectorateAndCategoryAndActiveTrueOrderBySortOrderAsc(
            String dg, String category
    );
    List<OperationalTypeConfig> findByDirectorateAndCategoryAndPhaseTypeAndActiveTrue(
            String dg, String category, String phase
    );
}