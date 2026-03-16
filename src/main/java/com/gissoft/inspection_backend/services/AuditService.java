package com.gissoft.inspection_backend.services;

import com.gissoft.inspection_backend.entity.AuditLog;
import com.gissoft.inspection_backend.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Fire-and-forget — never blocks the calling thread.
     */
    @Async
    public void log(String actor, String action, String resourceType,
                    String resourceId, Map<String, Object> diff, String remoteIp) {
        auditLogRepository.save(AuditLog.builder()
                .actor(actor)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .diffJson(diff)
                .remoteIp(remoteIp)
                .build());
    }

    @Async
    public void log(String actor, String action, String resourceType, String resourceId) {
        log(actor, action, resourceType, resourceId, null, null);
    }

    public Page<AuditLog> search(String actor, String resourceType,
                                 OffsetDateTime from, OffsetDateTime to,
                                 Pageable pageable) {
        // Pass empty string instead of null — Neon PostgreSQL cannot infer
        // the type of null parameters in JPQL queries
        String actorParam = actor != null ? actor : "";
        String resourceTypeParam = resourceType != null ? resourceType : "";

        // Default date range: epoch to far future if not provided
        OffsetDateTime fromParam = from != null ? from : OffsetDateTime.parse("2000-01-01T00:00:00Z");
        OffsetDateTime toParam = to != null ? to : OffsetDateTime.parse("2099-12-31T23:59:59Z");

        return auditLogRepository.search(actorParam, resourceTypeParam,
                fromParam, toParam, pageable);
    }
}