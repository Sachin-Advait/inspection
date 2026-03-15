package com.gissoft.inspection_backend.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "audit_log",
       indexes = {
           @Index(name = "idx_audit_actor",    columnList = "actor"),
           @Index(name = "idx_audit_resource", columnList = "resource_type, resource_id"),
           @Index(name = "idx_audit_created",  columnList = "created_at")
       })
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 80)
    private String actor;

    /** CREATE | UPDATE | DELETE | PUBLISH | APPROVE | REJECT | LOGIN … */
    @Column(nullable = false, length = 40)
    private String action;

    @Column(name = "resource_type", nullable = false, length = 60)
    private String resourceType;

    @Column(name = "resource_id", length = 80)
    private String resourceId;

    @Type(JsonBinaryType.class)
    @Column(name = "diff_json", columnDefinition = "jsonb")
    private Map<String, Object> diffJson;

    @Column(name = "remote_ip", length = 45)
    private String remoteIp;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
