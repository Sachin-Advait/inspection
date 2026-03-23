package com.gissoft.inspection_backend.repository;

import com.gissoft.inspection_backend.entity.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, UUID> {

    List<Notice> findByEntityIdOrderByCreatedAtDesc(UUID entityId);

    List<Notice> findByInspectionIdOrderByCreatedAtDesc(UUID inspectionId);

    @Query("""
            SELECT n FROM Notice n
            WHERE (:noticeType    IS NULL OR n.noticeType    = :noticeType)
              AND (:status        IS NULL OR n.status        = :status)
              AND (:paymentStatus IS NULL OR n.paymentStatus = :paymentStatus)
            ORDER BY n.createdAt DESC
            """)
    Page<Notice> findByFilters(@Param("noticeType") String noticeType,
                               @Param("status") String status,
                               @Param("paymentStatus") String paymentStatus,
                               Pageable pageable);

    long countByPaymentStatus(String paymentStatus);
}