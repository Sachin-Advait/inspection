package com.gissoft.inspection_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notice")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "notice_no", unique = true, length = 40)
    private String noticeNo;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "inspection_id", nullable = false)
    private UUID inspectionId;

    /** WARNING | FINE | CLOSURE */
    @Column(name = "notice_type", nullable = false, length = 30)
    private String noticeType;

    @Column(name = "fine_amount")
    private Long fineAmount;

    /** DRAFT | PENDING_APPROVAL | APPROVED | SENT | SERVED */
    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "served_at")
    private OffsetDateTime servedAt;

    @Column(name = "storage_key_pdf", length = 500)
    private String storageKeyPdf;

    @Column(name = "payment_ref", length = 80)
    private String paymentRef;

    /** UNPAID | PAID | WAIVED */
    @Column(name = "payment_status", length = 30)
    private String paymentStatus;

    @Column(name = "generated_by", length = 80)
    private String generatedBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
