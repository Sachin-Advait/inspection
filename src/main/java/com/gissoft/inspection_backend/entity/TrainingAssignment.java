package com.gissoft.inspection_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
        name = "training_assignments",
        indexes = {
                @Index(name = "idx_assignment_user", columnList = "user_id"),
                @Index(name = "idx_assignment_training", columnList = "training_id"),
                @Index(name = "idx_assignment_status", columnList = "status"),
                @Index(name = "idx_assignment_user_training", columnList = "user_id, training_id")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;   // 🔁 Mongo String → SQL Long

    /* =========================
       RELATION FIELDS
       ========================= */

    @Column(name = "username", nullable = false)
    private String username;   // (Keep String if users are external / JWT based)

    @Column(name = "training_id", nullable = false)
    private Long trainingId; // 🔁 FK to TrainingMaterial.id

    /* =========================
       PROGRESS TRACKING
       ========================= */

    @Column(nullable = false)
    private Integer progress; // percentage (0–100)

    @Column(nullable = false)
    private String status; // not-started, in-progress, completed

    /* =========================
       DATES
       ========================= */

    @Column(name = "due_date")
    private Instant dueDate;

    @Column(name = "assigned_at", updatable = false)
    private Instant assignedAt;

    /* =========================
       AUTO AUDIT
       ========================= */

    @PrePersist
    protected void onCreate() {
        this.assignedAt = Instant.now();
        if (this.progress == null) this.progress = 0;
        if (this.status == null) this.status = "not-started";
    }
}
