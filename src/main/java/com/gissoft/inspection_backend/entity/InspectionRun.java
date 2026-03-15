package com.gissoft.inspection_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "inspection_run")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InspectionRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "task_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "answers", "approvalRequests"})
    private Task task;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "entity_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private EntityMaster entity;

    @Column(name = "checklist_template_id", nullable = false)
    private UUID checklistTemplateId;

    @Column(name = "checklist_version", nullable = false)
    private int checklistVersion;

    @Column(name = "started_by", nullable = false, length = 80)
    private String startedBy;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    /**
     * PASS | FAIL | CONDITIONAL
     */
    @Column(length = 30)
    private String outcome;

    @Column(name = "summary_note", length = 800)
    private String summaryNote;

    @OneToMany(mappedBy = "inspection", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnoreProperties({"inspection"})
    private List<InspectionAnswer> answers = new ArrayList<>();

    @OneToMany(mappedBy = "inspection", cascade = CascadeType.ALL)
    @Builder.Default
    @JsonIgnoreProperties({"inspection"})
    private List<ApprovalRequest> approvalRequests = new ArrayList<>();
}