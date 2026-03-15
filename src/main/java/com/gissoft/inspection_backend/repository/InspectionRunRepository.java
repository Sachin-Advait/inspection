package com.gissoft.inspection_backend.repository;

import com.gissoft.inspection_backend.entity.InspectionRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InspectionRunRepository extends JpaRepository<InspectionRun, UUID> {

    List<InspectionRun> findByEntityIdOrderByStartedAtDesc(UUID entityId);

    /** Returns any in-progress (not yet submitted) inspection for a task. */
    Optional<InspectionRun> findByTaskIdAndSubmittedAtIsNull(UUID taskId);

    @Query("""
        SELECT i FROM InspectionRun i
        WHERE i.startedBy = :inspector
          AND i.startedAt BETWEEN :from AND :to
        ORDER BY i.startedAt DESC
        """)
    List<InspectionRun> findByInspectorAndPeriod(@Param("inspector") String inspector,
                                                  @Param("from")      OffsetDateTime from,
                                                  @Param("to")        OffsetDateTime to);

    @Query("SELECT COUNT(i) FROM InspectionRun i WHERE i.startedAt >= :since")
    long countSince(@Param("since") OffsetDateTime since);

    @Query("SELECT COUNT(i) FROM InspectionRun i WHERE i.outcome = 'FAIL' AND i.startedAt >= :since")
    long countFailsSince(@Param("since") OffsetDateTime since);
}
