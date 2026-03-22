package com.gissoft.inspection_backend.controller;

import com.gissoft.inspection_backend.entity.PhaseConfig;
import com.gissoft.inspection_backend.services.PhaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/phases")
@RequiredArgsConstructor
public class PhaseController {

    private final PhaseService phaseService;

    // ✅ GET PHASES (Track 1)
    @GetMapping
    public List<PhaseConfig> getPhases(
            @RequestParam String dg,
            @RequestParam String category
    ) {
        return phaseService.getPhases(dg, category);
    }

    // ✅ CREATE
    @PostMapping
    public PhaseConfig create(@RequestBody PhaseConfig phase) {
        return phaseService.create(phase);
    }

    // ✅ BULK SAVE
    @PostMapping("/bulk")
    public List<PhaseConfig> bulkSave(@RequestBody List<PhaseConfig> phases) {
        return phaseService.saveAll(phases);
    }
}