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

    /** Fire-and-forget — never blocks the calling thread. */
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
        return auditLogRepository.search(actor, resourceType, from, to, pageable);
    }
}
