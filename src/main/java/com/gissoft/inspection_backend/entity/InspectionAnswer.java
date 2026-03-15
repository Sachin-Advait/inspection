package com.gissoft.inspection_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "inspection_answer",
       uniqueConstraints = @UniqueConstraint(columnNames = {"inspection_id","question_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InspectionAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspection_id", nullable = false)
    private InspectionRun inspection;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(nullable = false, length = 120)
    private String answer;

    @Column(length = 400)
    private String note;
}
