package com.gissoft.inspection_backend.dto;

import jakarta.validation.constraints.NotBlank;

public class PermitDto {

    public record CreatePermitRequest(
            @NotBlank String category,   // ROAD | LIGHTS | BUILDING
            String contractor,
            String contact,
            String area,
            Double lat,
            Double lon
    ) {}
}
