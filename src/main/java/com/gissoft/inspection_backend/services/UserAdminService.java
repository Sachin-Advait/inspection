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

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final AppUserRepository userRepo;
    private final AuditService      auditService;
    private final PasswordEncoder   passwordEncoder;

    // ── List ──────────────────────────────────────────────────────────────────

    public Page<AppUser> list(Pageable pageable) {
        return userRepo.findAll(pageable);
    }

    public AppUser findById(UUID id) {
        return userRepo.findById(id)
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
        auditService.log(actor, "CREATE_USER", "AppUser", user.getId().toString());
        return user;
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Transactional
    public AppUser update(UUID id, UpdateUserRequest req, String actor) {
        AppUser user = findById(id);

        if (req.role()               != null) user.setRole(req.role());
        if (req.fullName()           != null) user.setFullName(req.fullName());
        if (req.phone()              != null) user.setPhone(req.phone());
        if (req.dgAccess()           != null) user.setDgAccess(req.dgAccess());
        if (req.fineLimit()          != null) user.setFineLimit(req.fineLimit());
        if (req.supervisorFineLimit()!= null) user.setSupervisorFineLimit(req.supervisorFineLimit());
        if (req.managerFineLimit()   != null) user.setManagerFineLimit(req.managerFineLimit());
        user.setCanCloseCases(req.canCloseCases());

        if (req.newPassword() != null && !req.newPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        }

        user = userRepo.save(user);
        auditService.log(actor, "UPDATE_USER", "AppUser", id.toString());
        return user;
    }

    // ── Deactivate ────────────────────────────────────────────────────────────

    @Transactional
    public void deactivate(UUID id, String actor) {
        AppUser user = findById(id);
        user.setActive(false);
        userRepo.save(user);
        auditService.log(actor, "DEACTIVATE_USER", "AppUser", id.toString());
    }
}
