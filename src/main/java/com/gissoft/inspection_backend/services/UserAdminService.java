package com.gissoft.inspection_backend.services;

import com.gissoft.inspection_backend.dto.UserDto.CreateUserRequest;
import com.gissoft.inspection_backend.dto.UserDto.UpdateUserRequest;
import com.gissoft.inspection_backend.entity.AppUser;
import com.gissoft.inspection_backend.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final AppUserRepository userRepo;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;

    // ── List ──────────────────────────────────────────────────────────────────

    public Page<AppUser> list(Pageable pageable) {
        return userRepo.findAll(pageable);
    }

    public AppUser findById(UUID id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    public AppUser findByUsername(String id) {
        return userRepo.findByUsername(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    public List<AppUser> getInspectors() {
        return userRepo.findByRoleAndActiveTrue("INSPECTOR");
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Transactional
    public AppUser create(CreateUserRequest req, String actor) {
        if (userRepo.existsByUsername(req.username())) {
            throw new IllegalArgumentException("Username already taken: " + req.username());
        }

        AppUser user = AppUser.builder()
                .username(req.username())
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(req.role())
                .fullName(req.fullName())
                .phone(req.phone())
                .dgAccess(req.dgAccess())
                .fineLimit(req.fineLimit())
                .supervisorFineLimit(req.supervisorFineLimit())
                .managerFineLimit(req.managerFineLimit())
                .canCloseCases(req.canCloseCases())
                .active(true)
                .build();

        user = userRepo.save(user);

        // ✅ AUDIT
        auditService.log(actor, "CREATE_USER", "AppUser", user.getId().toString());

        return user;
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Transactional
    public AppUser update(UUID id, UpdateUserRequest req, String actor) {
        AppUser user = findById(id);

        Map<String, Object> changes = new HashMap<>();

        if (req.role() != null) {
            changes.put("role_from", user.getRole());
            changes.put("role_to", req.role());
            user.setRole(req.role());
        }

        if (req.fullName() != null) {
            changes.put("fullName_from", user.getFullName());
            changes.put("fullName_to", req.fullName());
            user.setFullName(req.fullName());
        }

        if (req.phone() != null) {
            changes.put("phone_from", user.getPhone());
            changes.put("phone_to", req.phone());
            user.setPhone(req.phone());
        }

        if (req.dgAccess() != null) {
            changes.put("dgAccess_from", user.getDgAccess());
            changes.put("dgAccess_to", req.dgAccess());
            user.setDgAccess(req.dgAccess());
        }

        if (req.fineLimit() != null) {
            changes.put("fineLimit_from", user.getFineLimit());
            changes.put("fineLimit_to", req.fineLimit());
            user.setFineLimit(req.fineLimit());
        }

        if (req.supervisorFineLimit() != null) {
            changes.put("supervisorFineLimit_from", user.getSupervisorFineLimit());
            changes.put("supervisorFineLimit_to", req.supervisorFineLimit());
            user.setSupervisorFineLimit(req.supervisorFineLimit());
        }

        if (req.managerFineLimit() != null) {
            changes.put("managerFineLimit_from", user.getManagerFineLimit());
            changes.put("managerFineLimit_to", req.managerFineLimit());
            user.setManagerFineLimit(req.managerFineLimit());
        }

        changes.put("canCloseCases_from", user.isCanCloseCases());
        changes.put("canCloseCases_to", req.canCloseCases());
        user.setCanCloseCases(req.canCloseCases());

        if (req.newPassword() != null && !req.newPassword().isBlank()) {
            changes.put("password", "UPDATED");
            user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        }

        user = userRepo.save(user);

        // ✅ AUDIT with diff
        auditService.log(actor, "UPDATE_USER", "AppUser", id.toString(), changes);

        return user;
    }

    // ── Deactivate ────────────────────────────────────────────────────────────

    @Transactional
    public void deactivate(UUID id, String actor) {
        AppUser user = findById(id);

        user.setActive(false);
        userRepo.save(user);

        // ✅ AUDIT
        auditService.log(actor, "DEACTIVATE_USER", "AppUser", id.toString());
    }
}