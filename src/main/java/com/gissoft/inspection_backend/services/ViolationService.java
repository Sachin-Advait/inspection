package com.gissoft.inspection_backend.services;

import com.gissoft.inspection_backend.entity.FineRule;
import com.gissoft.inspection_backend.entity.ViolationCode;
import com.gissoft.inspection_backend.repository.FineRuleRepository;
import com.gissoft.inspection_backend.repository.ViolationCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ViolationService {

    private final ViolationCodeRepository violationCodeRepo;
    private final FineRuleRepository      fineRuleRepo;
    private final AuditService            auditService;

    // ── Violation codes ───────────────────────────────────────────────────────

    public List<ViolationCode> listCodes() {
        return violationCodeRepo.findAll();
    }

    @Transactional
    public ViolationCode createCode(ViolationCode req, String actor) {
        if (violationCodeRepo.existsByCode(req.getCode())) {
            throw new IllegalArgumentException("Code already exists: " + req.getCode());
        }
        req.setActive(true);
        ViolationCode vc = violationCodeRepo.save(req);
        auditService.log(actor, "CREATE", "ViolationCode", vc.getId().toString());
        return vc;
    }

    @Transactional
    public ViolationCode updateCode(UUID id, ViolationCode req, String actor) {
        ViolationCode vc = violationCodeRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Code not found: " + id));
        vc.setDescription(req.getDescription());
        vc.setSeverity(req.getSeverity());
        vc.setDefaultAction(req.getDefaultAction());
        vc.setLegalRef(req.getLegalRef());
        vc.setActive(req.isActive());
        vc = violationCodeRepo.save(vc);
        auditService.log(actor, "UPDATE", "ViolationCode", id.toString());
        return vc;
    }

    // ── Fine rules ────────────────────────────────────────────────────────────

    public List<FineRule> listFineRules() {
        return fineRuleRepo.findAll();
    }

    @Transactional
    public FineRule upsertFineRule(FineRule req, String actor) {
        FineRule rule = fineRuleRepo.findByViolationCode(req.getViolationCode())
                .orElse(FineRule.builder().violationCode(req.getViolationCode()).build());
        rule.setBaseFine(req.getBaseFine());
        rule.setMaxFine(req.getMaxFine());
        rule.setApprovalRequired(req.getApprovalRequired());
        rule = fineRuleRepo.save(rule);
        auditService.log(actor, "UPSERT", "FineRule", rule.getId().toString());
        return rule;
    }
}
