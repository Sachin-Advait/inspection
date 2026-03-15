package com.gissoft.inspection_backend.repository;

import com.gissoft.inspection_backend.entity.MessageLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
public interface MessageLogRepository extends JpaRepository<MessageLog, UUID> {

    @Query("""
        SELECT m FROM MessageLog m
        WHERE (:channel IS NULL OR m.channel = :channel)
          AND (:status  IS NULL OR m.status  = :status)
          AND m.createdAt BETWEEN :from AND :to
        ORDER BY m.createdAt DESC
        """)
    Page<MessageLog> findByFilters(@Param("channel") String channel,
                                    @Param("status")  String status,
                                    @Param("from")    OffsetDateTime from,
                                    @Param("to")      OffsetDateTime to,
                                    Pageable pageable);
}
