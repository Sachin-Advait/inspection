package com.gissoft.inspection_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TrainingEngagementDTO {

    private UUID userId;      // stays String (external user/JWT)
    private String learner;

    private Long trainingId;    // ✅ SQL FK
    private String video;       // training title

    private int progress;
    private String status;
}