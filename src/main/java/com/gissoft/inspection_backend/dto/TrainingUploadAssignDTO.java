package com.gissoft.inspection_backend.dto;

import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class TrainingUploadAssignDTO {

    /* =========================
       TRAINING META
       ========================= */
    private String title;
    private String type;
    private String duration;

    // Cloudinary
    private String cloudinaryPublicId;
    private String cloudinaryUrl;
    private String cloudinaryResourceType;
    private String cloudinaryFormat;

    /* =========================
       ASSIGNMENT
       ========================= */
    private List<String> usernames;
    private Instant dueDate;
}