package com.gissoft.inspection_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "fine_rule")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FineRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "violation_code", unique = true, nullable = false, length = 40)
    private String violationCode;

    @Column(name = "base_fine", nullable = false)
    private long baseFine;

    @Column(name = "max_fine", nullable = false)
    private long maxFine;

    /** NONE | SUPERVISOR | MANAGER */
    @Column(name = "approval_required", nullable = false, length = 20)
    private String approvalRequired;
}
