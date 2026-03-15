package com.gissoft.inspection_backend.repository;

import com.gissoft.inspection_backend.entity.OracleOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OracleOutboxEventRepository extends JpaRepository<OracleOutboxEvent, UUID> {

    @Query("""
        SELECT o FROM OracleOutboxEvent o
        WHERE o.status = 'PENDING'
          AND o.attempts < :maxAttempts
        ORDER BY o.createdAt ASC
        LIMIT :batchSize
        """)
    List<OracleOutboxEvent> findPendingBatch(@Param("maxAttempts") int maxAttempts,
                                              @Param("batchSize")   int batchSize);

    Optional<OracleOutboxEvent> findByIdempotencyKey(String idempotencyKey);

    long countByStatus(String status);
}
