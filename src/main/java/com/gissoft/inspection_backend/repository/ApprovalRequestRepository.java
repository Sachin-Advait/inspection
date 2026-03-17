package com.gissoft.inspection_backend.repository;

import com.gissoft.inspection_backend.entity.ApprovalRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, UUID> {

    // ── Fetch with full inspection + entity + task in one query ───────────────
    // JOIN FETCH forces Hibernate to load everything eagerly so Jackson
    // never hits an uninitialized proxy when serializing.
    // Note: JOIN FETCH + Pageable requires a separate count query to avoid
    // the "HHH90003004: firstResult/maxResults specified with collection fetch"
    // warning — so we split into a data query + count query.

    @Query("""
        SELECT a FROM ApprovalRequest a
        JOIN FETCH a.inspection i
        JOIN FETCH i.entity e
        JOIN FETCH i.task t
        WHERE a.status = 'PENDING'
          AND (:level IS NULL OR a.requiredLevel = :level)
        ORDER BY a.createdAt ASC
        """)
    List<ApprovalRequest> findPendingWithDetails(@Param("level") String level, Pageable pageable);

    @Query("""
        SELECT COUNT(a) FROM ApprovalRequest a
        WHERE a.status = 'PENDING'
          AND (:level IS NULL OR a.requiredLevel = :level)
        """)
    long countPending(@Param("level") String level);

    // Keep old method signature so existing code doesn't break —
    // wire it to findPendingWithDetails via default method
    default Page<ApprovalRequest> findPending(String level, Pageable pageable) {
        List<ApprovalRequest> content = findPendingWithDetails(level, pageable);
        long total = countPending(level);
        return new PageImpl<>(content, pageable, total);
    }

    List<ApprovalRequest> findByInspectionIdOrderByCreatedAtDesc(UUID inspectionId);

    long countByStatus(String status);
}