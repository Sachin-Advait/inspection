package com.gissoft.inspection_backend.workflow;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.gissoft.inspection_backend.entity.Notice;
import com.gissoft.inspection_backend.repository.EntityMasterRepository;
import com.gissoft.inspection_backend.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * Flowable JavaDelegate — fires when the "Generate Notice" service task executes.
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class GenerateNoticeService implements JavaDelegate {

    private final NoticeRepository noticeRepository;
    private final EntityMasterRepository entityRepository;
    private final Cloudinary cloudinary;

    @Override
    public void execute(DelegateExecution execution) {

        String inspectionId = (String) execution.getVariable("inspectionId");
        String entityId = (String) execution.getVariable("entityId");
        String noticeType = (String) execution.getVariable("noticeType");
        Long fineAmount = toLong(execution.getVariable("fineAmount"));
        String generatedBy = (String) execution.getVariable("generatedBy");

        log.info("Generating notice — type: {}, entity: {}, inspection: {}",
                noticeType, entityId, inspectionId);

        String noticeNo = buildNoticeNo(noticeType);

        byte[] pdfBytes = buildMinimalPdf(noticeNo, noticeType, fineAmount);

        String pdfUrl;

        try {

            Map uploadResult = cloudinary.uploader().upload(
                    new ByteArrayInputStream(pdfBytes),
                    ObjectUtils.asMap(
                            "resource_type", "raw",
                            "public_id", "notices/" + entityId + "/" + noticeNo
                    )
            );

            pdfUrl = uploadResult.get("secure_url").toString();

        } catch (Exception ex) {

            log.error("Cloudinary upload failed for notice {}: {}", noticeNo, ex.getMessage());
            throw new RuntimeException("Notice PDF upload failed", ex);
        }

        Notice notice = Notice.builder()
                .noticeNo(noticeNo)
                .entityId(UUID.fromString(entityId))
                .inspectionId(UUID.fromString(inspectionId))
                .noticeType(noticeType)
                .fineAmount(fineAmount)
                .status("DRAFT")
                .storageKeyPdf(pdfUrl)   // now storing URL instead
                .paymentStatus("UNPAID")
                .generatedBy(generatedBy)
                .build();

        notice = noticeRepository.save(notice);

        execution.setVariable("noticeId", notice.getId().toString());
        execution.setVariable("noticeNo", noticeNo);

        log.info("Notice generated successfully: {}", noticeNo);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String buildNoticeNo(String type) {

        String prefix = switch (type != null ? type.toUpperCase() : "NTC") {
            case "WARNING" -> "WRN";
            case "FINE" -> "FIN";
            case "CLOSURE" -> "CLO";
            default -> "NTC";
        };

        String date = OffsetDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        String uid = UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();

        return prefix + "-" + date + "-" + uid;
    }

    /**
     * Minimal valid PDF stub.
     */
    private byte[] buildMinimalPdf(String noticeNo, String type, Long fineAmount) {

        String content = "%PDF-1.4\n"
                + "1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n"
                + "2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n"
                + "3 0 obj<</Type/Page/MediaBox[0 0 595 842]/Parent 2 0 R/Contents 4 0 R>>endobj\n"
                + "4 0 obj<</Length 80>>stream\n"
                + "BT /F1 14 Tf 50 780 Td (Notice: " + noticeNo + ") Tj\n"
                + "0 -24 Td (Type: " + type + ") Tj\n"
                + (fineAmount != null ? "0 -24 Td (Fine: " + fineAmount + ") Tj\n" : "")
                + "ET\nendstream endobj\n"
                + "xref\n0 5\ntrailer<</Size 5/Root 1 0 R>>\nstartxref\n0\n%%EOF";

        return content.getBytes();
    }

    private Long toLong(Object value) {

        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer i) return i.longValue();
        if (value instanceof Number n) return n.longValue();

        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}