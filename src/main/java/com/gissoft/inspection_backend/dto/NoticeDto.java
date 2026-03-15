package com.gissoft.inspection_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class NoticeDto {

    public record GenerateRequest(
            @NotNull  UUID   inspectionId,
            @NotNull  UUID   entityId,
            @NotBlank String noticeType,   // WARNING | FINE | CLOSURE
            Long   fineAmount,
            String lang                    // EN | AR  (defaults EN)
    ) {}

    public record SendRequest(
            String channel     // WHATSAPP | SMS — optional, defaults to WHATSAPP
    ) {}
}
