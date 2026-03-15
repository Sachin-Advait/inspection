package com.gissoft.inspection_backend.services;

import com.gissoft.inspection_backend.dto.ReportDto.ReportRequest;
import com.gissoft.inspection_backend.repository.InspectionRunRepository;
import com.gissoft.inspection_backend.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportingService {

    private final InspectionRunRepository inspectionRunRepo;
    private final NoticeRepository        noticeRepo;

    // ── Build stats map ───────────────────────────────────────────────────────

    public Map<String, Object> buildReport(ReportRequest req) {
        OffsetDateTime from = req.from() != null ? req.from() : OffsetDateTime.now().minusMonths(1);

        long total    = inspectionRunRepo.countSince(from);
        long fails    = inspectionRunRepo.countFailsSince(from);
        long passes   = total - fails;
        double rate   = total > 0 ? Math.round((double) passes / total * 1000) / 10.0 : 0;

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("generatedAt",      OffsetDateTime.now().toString());
        report.put("dateFrom",         from.toString());
        report.put("dateTo",           req.to() != null ? req.to().toString() : "now");
        report.put("dg",               req.dg() != null ? req.dg() : "ALL");
        report.put("totalInspections", total);
        report.put("passed",           passes);
        report.put("failed",           fails);
        report.put("passRatePct",      rate);
        report.put("finesIssued",      noticeRepo.count());
        report.put("finesPaid",        noticeRepo.countByPaymentStatus("PAID"));
        report.put("finesUnpaid",      noticeRepo.countByPaymentStatus("UNPAID"));
        return report;
    }

    // ── Excel export ──────────────────────────────────────────────────────────

    public byte[] exportExcel(ReportRequest req) throws Exception {
        Map<String, Object> data = buildReport(req);

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Inspection Report");

            // Header style
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row header = sheet.createRow(0);
            String[] cols = { "Metric", "Value" };
            for (int i = 0; i < cols.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(cols[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowNum = 1;
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(entry.getKey());
                row.createCell(1).setCellValue(
                        entry.getValue() != null ? entry.getValue().toString() : "");
            }

            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    // ── CSV export ────────────────────────────────────────────────────────────

    public byte[] exportCsv(ReportRequest req) {
        Map<String, Object> data = buildReport(req);
        StringBuilder sb = new StringBuilder("metric,value\n");
        data.forEach((k, v) ->
                sb.append(k).append(",").append(v != null ? v : "").append("\n"));
        return sb.toString().getBytes();
    }
}
