package com.gissoft.inspection_backend.repository;

import com.gissoft.inspection_backend.entity.TrainingMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrainingMaterialRepository
        extends JpaRepository<TrainingMaterial, Long> {

    List<TrainingMaterial> findAllByActiveTrue();


    Optional<TrainingMaterial> findByIdAndActiveTrue(Long id);

    boolean existsByCloudinaryPublicIdAndActiveTrue(String cloudinaryPublicId);

    List<TrainingMaterial> findByCloudinaryResourceTypeAndDurationAndActiveTrue(
            String cloudinaryResourceType,
            String duration
    );
}


