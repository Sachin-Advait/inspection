package com.gissoft.inspection_backend.dto;

import java.time.OffsetDateTime;

public class ReportDto {

    public record ReportRequest(
            OffsetDateTime from,
            OffsetDateTime to,
            String dg,
            String category,
            String area,
            String inspector,
            String format      // JSON | EXCEL | CSV
    ) {}
}
