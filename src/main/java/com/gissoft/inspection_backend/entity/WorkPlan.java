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
@Table(name = "work_plan")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** DAILY | WEEKLY */
    @Column(name = "plan_type", nullable = false, length = 20)
    private String planType;

    @Column(name = "date_from", nullable = false)
    private OffsetDateTime dateFrom;

    @Column(name = "date_to", nullable = false)
    private OffsetDateTime dateTo;

    @Column(nullable = false, length = 20)
    private String dg;

    @Column(length = 60)
    private String category;

    @Column(length = 120)
    private String area;

    /** DRAFT | PUBLISHED | CLOSED */
    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "created_by", nullable = false, length = 80)
    private String createdBy;

    @Type(JsonBinaryType.class)
    @Column(name = "sla_rules_json", columnDefinition = "jsonb")
    private Map<String, Object> slaRulesJson;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
