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

    @Query("""
        SELECT a FROM AuditLog a
        WHERE (:actor        IS NULL OR a.actor        = :actor)
          AND (:resourceType IS NULL OR a.resourceType = :resourceType)
          AND (:from         IS NULL OR a.createdAt   >= :from)
          AND (:to           IS NULL OR a.createdAt   <= :to)
        ORDER BY a.createdAt DESC
        """)
    Page<AuditLog> search(@Param("actor")        String actor,
                           @Param("resourceType") String resourceType,
                           @Param("from")         OffsetDateTime from,
                           @Param("to")           OffsetDateTime to,
                           Pageable pageable);
}
