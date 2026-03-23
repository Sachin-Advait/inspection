package com.gissoft.inspection_backend.repository;

import com.gissoft.inspection_backend.entity.EntityMaster;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EntityMasterRepository extends JpaRepository<EntityMaster, UUID> {

    Optional<EntityMaster> findByExternalRef(String externalRef);

    /**
     * Search entities — always excludes COMPLETED.
     * Only returns ACTIVE and OVERDUE entities.
     * OVERDUE shown first, then ACTIVE, ordered by nextDueAt.
     */
    @Query("""
            SELECT e FROM EntityMaster e
            WHERE e.complianceFlag IN ('ACTIVE', 'OVERDUE')
              AND (:dg       IS NULL OR e.directorate = :dg)
              AND (:category IS NULL OR e.category    = :category)
              AND (
                    :query IS NULL OR :query = ''
                    OR LOWER(e.externalRef) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(e.name)        LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(e.ownerPhone)  LIKE LOWER(CONCAT('%', :query, '%'))
                  )
            ORDER BY
              CASE e.complianceFlag
                WHEN 'OVERDUE' THEN 1
                WHEN 'ACTIVE'  THEN 2
                ELSE 3
              END,
              e.nextDueAt ASC NULLS LAST
            """)
    Page<EntityMaster> search(@Param("dg") String dg,
                              @Param("category") String category,
                              @Param("query") String query,
                              Pageable pageable);

    @Query("""
            SELECT e FROM EntityMaster e
            WHERE e.directorate = 'HEALTH'
              AND (:category IS NULL OR e.category = :category)
              AND e.nextDueAt IS NOT NULL
            ORDER BY e.nextDueAt ASC
            """)
    List<EntityMaster> findHealthDueList(@Param("category") String category,
                                         Pageable pageable);
}