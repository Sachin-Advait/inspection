package com.gissoft.inspection_backend.services;

import com.gissoft.inspection_backend.dto.ChecklistDto.*;
import com.gissoft.inspection_backend.entity.*;
import com.gissoft.inspection_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChecklistService {

    private final ChecklistTemplateRepository templateRepo;
    private final ChecklistSectionRepository  sectionRepo;
    private final ChecklistQuestionRepository questionRepo;
    private final AuditService                auditService;

    // ── Mobile: get active checklist ─────────────────────────────────────────

    public ChecklistTemplate getActive(String dg, String category, String phaseType) {
        return templateRepo.findActive(dg, category, phaseType)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No active checklist for: " + dg + "/" + category + "/" + phaseType));
    }

    // ── Admin: list ───────────────────────────────────────────────────────────

    public List<ChecklistTemplate> list(String dg, String category, String status) {
        return templateRepo.findByFilters(dg, category, status);
    }

    public ChecklistTemplate findById(UUID id) {
        return templateRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + id));
    }

    // ── Admin: create template ────────────────────────────────────────────────

    @Transactional
    public ChecklistTemplate createTemplate(CreateTemplateRequest req, String actor) {
        int next = templateRepo.findMaxVersion(req.dg(), req.category(), req.phaseType()) + 1;

        ChecklistTemplate t = ChecklistTemplate.builder()
                .name(req.name())
                .dg(req.dg())
                .category(req.category())
                .phaseType(req.phaseType())
                .version(next)
                .status("DRAFT")
                .build();

        t = templateRepo.save(t);
        auditService.log(actor, "CREATE", "ChecklistTemplate", t.getId().toString());
        return t;
    }

    // ── Admin: add section ────────────────────────────────────────────────────

    @Transactional
    public ChecklistSection addSection(UUID templateId, AddSectionRequest req, String actor) {
        ChecklistTemplate template = findById(templateId);
        assertDraft(template);

        ChecklistSection section = ChecklistSection.builder()
                .template(template)
                .sortOrder(req.sortOrder())
                .title(req.title())
                .description(req.description())
                .build();

        section = sectionRepo.save(section);
        auditService.log(actor, "ADD_SECTION", "ChecklistTemplate", templateId.toString());
        return section;
    }

    // ── Admin: add question ───────────────────────────────────────────────────

    @Transactional
    public ChecklistQuestion addQuestion(UUID sectionId, AddQuestionRequest req, String actor) {
        ChecklistSection section = sectionRepo.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section not found: " + sectionId));
        assertDraft(section.getTemplate());

        ChecklistQuestion question = ChecklistQuestion.builder()
                .section(section)
                .sortOrder(req.sortOrder())
                .text(req.text())
                .answerType(req.answerType())
                .required(req.required())
                .validationsJson(req.validationsJson())
                .build();

        question = questionRepo.save(question);
        auditService.log(actor, "ADD_QUESTION", "ChecklistSection", sectionId.toString());
        return question;
    }

    // ── Admin: set rule on question ───────────────────────────────────────────

    @Transactional
    public ChecklistRule setRule(UUID questionId, SetRuleRequest req, String actor) {
        ChecklistQuestion question = questionRepo.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));
        assertDraft(question.getSection().getTemplate());

        ChecklistRule rule = question.getRule();
        if (rule == null) {
            rule = ChecklistRule.builder().question(question).build();
        }
        rule.setFailSeverity(req.failSeverity());
        rule.setEvidencePolicyJson(req.evidencePolicyJson());
        rule.setViolationCode(req.violationCode());
        rule.setDefaultAction(req.defaultAction());
        rule.setForceApprovalLevel(req.forceApprovalLevel());
        rule.setReinspectionSuggestionJson(req.reinspectionSuggestionJson());

        question.setRule(rule);
        questionRepo.save(question);

        auditService.log(actor, "SET_RULE", "ChecklistQuestion", questionId.toString());
        return rule;
    }

    // ── Admin: publish ────────────────────────────────────────────────────────

    @Transactional
    public ChecklistTemplate publish(UUID templateId, PublishRequest req, String actor) {
        ChecklistTemplate t = findById(templateId);
        assertDraft(t);

        t.setStatus("PUBLISHED");
        t.setReleaseNotes(req.releaseNotes());
        t = templateRepo.save(t);

        auditService.log(actor, "PUBLISH", "ChecklistTemplate", templateId.toString());
        return t;
    }

    // ── Admin: activate ───────────────────────────────────────────────────────

    @Transactional
    public ChecklistTemplate activate(UUID templateId, String actor) {
        ChecklistTemplate t = findById(templateId);
        if (!"PUBLISHED".equals(t.getStatus())) {
            throw new IllegalStateException("Only PUBLISHED templates can be activated");
        }

        // Retire existing ACTIVE for same DG/category/phase
        templateRepo.findActive(t.getDg(), t.getCategory(), t.getPhaseType())
                .ifPresent(existing -> {
                    existing.setStatus("RETIRED");
                    templateRepo.save(existing);
                });

        t.setStatus("ACTIVE");
        t = templateRepo.save(t);

        auditService.log(actor, "ACTIVATE", "ChecklistTemplate", templateId.toString());
        return t;
    }

    public ChecklistQuestion getQuestion(UUID questionId) {
        return questionRepo.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private void assertDraft(ChecklistTemplate t) {
        if (!"DRAFT".equals(t.getStatus())) {
            throw new IllegalStateException(
                    "Template is not in DRAFT status — cannot modify: " + t.getId());
        }
    }


}
