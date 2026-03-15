package com.gissoft.inspection_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskFilterRequest {

    private String dg;
    private String status;
    private String assignedTo;
    private UUID workPlanId;
    private String priority;
}
