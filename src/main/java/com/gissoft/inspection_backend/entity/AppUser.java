package com.gissoft.inspection_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "app_user")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 80)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /** INSPECTOR | SUPERVISOR | MANAGER | ADMIN */
    @Column(nullable = false, length = 40)
    private String role;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "full_name", length = 120)
    private String fullName;

    @Column(length = 50)
    private String phone;

    /** ALL | HEALTH | TECHNICAL */
    @Column(name = "dg_access", length = 40)
    private String dgAccess;

    @Column(name = "fine_limit")
    private Long fineLimit;

    @Column(name = "supervisor_fine_limit")
    private Long supervisorFineLimit;

    @Column(name = "manager_fine_limit")
    private Long managerFineLimit;

    @Column(name = "can_close_cases")
    private boolean canCloseCases = false;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
