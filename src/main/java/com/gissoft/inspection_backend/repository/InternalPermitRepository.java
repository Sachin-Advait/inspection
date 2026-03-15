package com.gissoft.inspection_backend.repository;

import com.gissoft.inspection_backend.entity.InternalPermit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InternalPermitRepository extends JpaRepository<InternalPermit, UUID> {

    Optional<InternalPermit> findByPermitNo(String permitNo);

    @Query("""
        SELECT p FROM InternalPermit p
        WHERE (:category IS NULL OR p.category = :category)
          AND (:status   IS NULL OR p.status   = :status)
          AND (:area     IS NULL OR p.area      = :area)
        ORDER BY p.createdAt DESC
        """)
    Page<InternalPermit> findByFilters(@Param("category") String category,
                                        @Param("status")   String status,
                                        @Param("area")     String area,
                                        Pageable pageable);
}
