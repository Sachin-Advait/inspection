package com.gissoft.inspection_backend.repository;

import com.gissoft.inspection_backend.entity.TrainingAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrainingAssignmentRepository
        extends JpaRepository<TrainingAssignment, Long> {

    List<TrainingAssignment> findByUserId(String userId);

    List<TrainingAssignment> findByTrainingId(Long trainingId);

    Optional<TrainingAssignment> findByUserIdAndTrainingId(
            String userId,
            Long trainingId
    );

    long countByTrainingId(Long trainingId);

    long countByTrainingIdAndStatus(Long trainingId, String status);

    void deleteByTrainingId(Long trainingId);
}

