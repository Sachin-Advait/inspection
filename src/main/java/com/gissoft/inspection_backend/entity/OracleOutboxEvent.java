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
@Table(name = "oracle_outbox_event",
       indexes = @Index(name = "idx_outbox_status", columnList = "status"))
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OracleOutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** RESULT_UPDATE | ENFORCEMENT_UPDATE | NEXT_DUE_UPDATE */
    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id", nullable = false)
    private EntityMaster entity;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 120)
    private String idempotencyKey;

    @Type(JsonBinaryType.class)
    @Column(name = "payload_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payloadJson;

    /** PENDING | PROCESSING | SENT | FAILED | DEAD_LETTER */
    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(name = "last_error", length = 800)
    private String lastError;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "last_attempt_at")
    private OffsetDateTime lastAttemptAt;
}
