package com.gissoft.inspection_backend.services;

import com.gissoft.inspection_backend.entity.AuditLog;
import com.gissoft.inspection_backend.repository.AuditLogRepository;
import com.gissoft.inspection_backend.repository.AppUserRepository;
import com.gissoft.inspection_backend.util.RequestContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final AppUserRepository userRepo;
    private final RequestContext requestContext;

    /**
     * ✅ MAIN METHOD (use this everywhere)
     */
    @Async
    public void log(String actor,
                    String action,
                    String resourceType,
                    String resourceId,
                    Map<String, Object> diff) {

        String role = userRepo.findByUsername(actor)
                .map(u -> u.getRole())
                .orElse("UNKNOWN");

        String ip = getSafeIp();

        Map<String, Object> enrichedDiff =
                diff != null ? new HashMap<>(diff) : new HashMap<>();

        enrichedDiff.put("role", role);

        auditLogRepository.save(AuditLog.builder()
                .actor(actor)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .diffJson(enrichedDiff)
                .remoteIp(ip)
                .build());
    }

    /**
     * ✅ SIMPLE VERSION
     */
    @Async
    public void log(String actor,
                    String action,
                    String resourceType,
                    String resourceId) {

        log(actor, action, resourceType, resourceId, null);
    }

    /**
     * ⚠️ OPTIONAL (manual override)
     */
    public void logWithContext(String actor,
                               String role,
                               String action,
                               String resourceType,
                               String resourceId,
                               Map<String, Object> diff,
                               String remoteIp) {

        Map<String, Object> enrichedDiff =
                diff != null ? new HashMap<>(diff) : new HashMap<>();

        if (role != null) {
            enrichedDiff.put("role", role);
        }

        auditLogRepository.save(AuditLog.builder()
                .actor(actor)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .diffJson(enrichedDiff)
                .remoteIp(remoteIp)
                .build());
    }

    /**
     * 🔍 SEARCH API
     */
    public Page<AuditLog> search(String actor,
                                 String resourceType,
                                 OffsetDateTime from,
                                 OffsetDateTime to,
                                 Pageable pageable) {

        String actorParam = actor != null ? actor : "";
        String resourceTypeParam = resourceType != null ? resourceType : "";

        OffsetDateTime fromParam = from != null
                ? from
                : OffsetDateTime.parse("2000-01-01T00:00:00Z");

        OffsetDateTime toParam = to != null
                ? to
                : OffsetDateTime.parse("2099-12-31T23:59:59Z");

        return auditLogRepository.search(
                actorParam,
                resourceTypeParam,
                fromParam,
                toParam,
                pageable
        );
    }

    /**
     * ✅ SAFE IP FETCH (fix for async issue)
     */
    private String getSafeIp() {
        try {
            return requestContext.getClientIp();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
}