package com.gissoft.inspection_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "entity_master")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EntityMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "external_ref", unique = true, nullable = false, length = 80)
    private String externalRef;

    /** HEALTH | TECHNICAL */
    @Column(nullable = false, length = 20)
    private String directorate;

    /** Restaurant | Salon | Building | Road | Lights … */
    @Column(nullable = false, length = 60)
    private String category;

    /** ORACLE | INTERNAL */
    @Column(name = "source_system", nullable = false, length = 20)
    private String sourceSystem;

    @Column(length = 255)
    private String name;

    @Column(name = "owner_name", length = 255)
    private String ownerName;

    @Column(name = "owner_phone", length = 50)
    private String ownerPhone;

    private Double lat;
    private Double lon;

    @Column(name = "last_inspection_at")
    private OffsetDateTime lastInspectionAt;

    @Column(name = "last_inspection_result", length = 40)
    private String lastInspectionResult;

    @Column(name = "next_due_at")
    private OffsetDateTime nextDueAt;

    @Column(name = "compliance_flag", length = 40)
    private String complianceFlag;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
