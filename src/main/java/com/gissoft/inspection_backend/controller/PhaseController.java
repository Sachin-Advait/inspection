package com.gissoft.inspection_backend.controller;

import com.gissoft.inspection_backend.entity.PhaseConfig;
import com.gissoft.inspection_backend.services.PhaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/admin/phases")
@RequiredArgsConstructor
public class PhaseController {

    private final PhaseService phaseService;

    // ✅ GET PHASES (no audit)
    @GetMapping
    public List<PhaseConfig> getPhases(
            @RequestParam String dg,
            @RequestParam String category
    ) {
        return phaseService.getPhases(dg, category);
    }

    // ✅ CREATE (with actor)
    @PostMapping
    public PhaseConfig create(@RequestBody PhaseConfig phase,
                              Principal principal) {
        return phaseService.create(phase, principal.getName());
    }

    // ✅ BULK SAVE (with actor)
    @PostMapping("/bulk")
    public List<PhaseConfig> bulkSave(@RequestBody List<PhaseConfig> phases,
                                      Principal principal) {
        return phaseService.saveAll(phases, principal.getName());
    }
}