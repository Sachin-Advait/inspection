package com.gissoft.inspection_backend.repository;

import com.gissoft.inspection_backend.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * Neon PostgreSQL cannot infer the type of null JPQL parameters.
     * Use COALESCE to avoid passing raw nulls — always pass a real value.
     * AuditService passes empty string "" for nulls on String params,
     * and epoch/far-future for date params.
     */
    @Query("""
            SELECT a FROM AuditLog a
            WHERE (COALESCE(:actor, '') = '' OR a.actor = :actor)
              AND (COALESCE(:resourceType, '') = '' OR a.resourceType = :resourceType)
              AND a.createdAt >= :from
              AND a.createdAt <= :to
            ORDER BY a.createdAt DESC
            """)
    Page<AuditLog> search(@Param("actor") String actor,
                          @Param("resourceType") String resourceType,
                          @Param("from") OffsetDateTime from,
                          @Param("to") OffsetDateTime to,
                          Pageable pageable);
}