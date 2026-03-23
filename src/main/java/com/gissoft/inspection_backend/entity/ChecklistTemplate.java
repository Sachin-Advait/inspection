package com.gissoft.inspection_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "checklist_template",
        uniqueConstraints = @UniqueConstraint(columnNames = {"dg", "category", "phase_type", "version"}))
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChecklistTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 20)
    private String dg;

    @Column(nullable = false, length = 60)
    private String category;

    @Column(name = "phase_type", nullable = false, length = 80)
    private String phaseType;

    @Column(nullable = false)
    private int version;

    /**
     * DRAFT | PUBLISHED | ACTIVE | RETIRED
     */
    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "release_notes", length = 800)
    private String releaseNotes;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    @JsonIgnoreProperties({"template"})
    private List<ChecklistSection> sections = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}