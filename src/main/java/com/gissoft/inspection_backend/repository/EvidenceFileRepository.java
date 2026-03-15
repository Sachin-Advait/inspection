package com.gissoft.inspection_backend.repository;

import com.gissoft.inspection_backend.entity.EvidenceFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EvidenceFileRepository extends JpaRepository<EvidenceFile, UUID> {

    List<EvidenceFile> findByInspectionIdOrderByCreatedAtAsc(UUID inspectionId);

    @Query("""
        SELECT e FROM EvidenceFile e
        WHERE (:entityId IS NULL OR e.entityId = :entityId)
          AND (:fileType IS NULL OR e.fileType  = :fileType)
        ORDER BY e.capturedAt DESC
        """)
    Page<EvidenceFile> findByFilters(@Param("entityId") UUID entityId,
                                      @Param("fileType")  String fileType,
                                      Pageable pageable);
}
