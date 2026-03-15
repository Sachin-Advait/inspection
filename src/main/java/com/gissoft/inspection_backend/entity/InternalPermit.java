package com.gissoft.inspection_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "internal_permit")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InternalPermit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "permit_no", unique = true, nullable = false, length = 40)
    private String permitNo;

    /** ROAD | LIGHTS | BUILDING */
    @Column(nullable = false, length = 60)
    private String category;

    @Column(length = 255)
    private String contractor;

    @Column(length = 80)
    private String contact;

    @Column(length = 120)
    private String area;

    /** DRAFT | ACTIVE | CLOSED | CANCELLED */
    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "current_phase", length = 80)
    private String currentPhase;

    private Double lat;
    private Double lon;

    /** Set after activation — links to entity_master */
    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "created_by", length = 80)
    private String createdBy;

    @Column(name = "activated_at")
    private OffsetDateTime activatedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
