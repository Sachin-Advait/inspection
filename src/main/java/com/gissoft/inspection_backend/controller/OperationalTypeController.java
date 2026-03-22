package com.gissoft.inspection_backend.controller;

import com.gissoft.inspection_backend.entity.OperationalTypeConfig;
import com.gissoft.inspection_backend.services.OperationalTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/operational-types")
@RequiredArgsConstructor
public class OperationalTypeController {

    private final OperationalTypeService service;

    @GetMapping
    public List<OperationalTypeConfig> get(
            @RequestParam String dg,
            @RequestParam String category,
            @RequestParam String phase
    ) {
        return service.get(dg, category, phase);
    }

    @PostMapping("/bulk")
    public List<OperationalTypeConfig> save(
            @RequestBody List<OperationalTypeConfig> list
    ) {
        return service.saveAll(list);
    }
}