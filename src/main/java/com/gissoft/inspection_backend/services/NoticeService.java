package com.gissoft.inspection_backend.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.gissoft.inspection_backend.dto.NoticeDto.GenerateRequest;
import com.gissoft.inspection_backend.dto.NoticeDto.SendRequest;
import com.gissoft.inspection_backend.entity.EntityMaster;
import com.gissoft.inspection_backend.entity.MessageLog;
import com.gissoft.inspection_backend.entity.Notice;
import com.gissoft.inspection_backend.repository.EntityMasterRepository;
import com.gissoft.inspection_backend.repository.MessageLogRepository;
import com.gissoft.inspection_backend.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NoticeService {

    private final NoticeRepository noticeRepo;
    private final EntityMasterRepository entityRepo;
    private final MessageLogRepository messageLogRepo;
    private final Cloudinary cloudinary;
    private final WhatsAppService whatsAppService;
    private final AuditService auditService;

    @Transactional
    public Notice generate(GenerateRequest req, String actor) throws Exception {

        EntityMaster entity = entityRepo.findById(req.entityId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Entity not found: " + req.entityId()));

        String noticeNo = buildNoticeNo(req.noticeType());
        byte[] pdfBytes = buildMinimalPdf(noticeNo, entity, req);

        String pdfUrl;

        try {
            var uploadResult = cloudinary.uploader().upload(
                    pdfBytes,
                    ObjectUtils.asMap(
                            "resource_type", "raw",
                            "public_id", "notices/" + req.entityId() + "/" + noticeNo
                    )
            );
            pdfUrl = uploadResult.get("secure_url").toString();

        } catch (Exception ex) {
            log.error("Cloudinary upload failed for notice {}: {}", noticeNo, ex.getMessage());
            throw new RuntimeException("Notice PDF upload failed", ex);
        }

        Notice notice = Notice.builder()
                .noticeNo(noticeNo)
                .entityId(req.entityId())
                .inspectionId(req.inspectionId())
                .noticeType(
                        req.noticeType() != null && !req.noticeType().isBlank()
                                ? req.noticeType()
                                : "WARNING"
                )
                .fineAmount(req.fineAmount())
                .status("DRAFT")
                .storageKeyPdf(pdfUrl)
                .paymentStatus("UNPAID")
                .generatedBy(actor)
                .build();

        notice = noticeRepo.save(notice);

        // ✅ AUDIT
        auditService.log(actor, "GENERATE_NOTICE", "Notice", notice.getId().toString());

        return notice;
    }

    @Transactional
    public Notice send(UUID noticeId, SendRequest req, String actor) {

        Notice notice = findById(noticeId);

        Notice finalNotice = notice;
        EntityMaster entity = entityRepo.findById(notice.getEntityId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Entity not found: " + finalNotice.getEntityId()));

        String channel = (req != null && req.channel() != null)
                ? req.channel().toUpperCase() : "WHATSAPP";

        String messageText = buildWhatsAppMessage(notice, entity);

        String providerMsgId = null;
        String deliveryStatus = "FAILED";

        if ("WHATSAPP".equals(channel)) {
            if (entity.getOwnerPhone() != null && !entity.getOwnerPhone().isBlank()) {
                providerMsgId = whatsAppService.sendMessage(
                        entity.getOwnerPhone(), messageText, actor);
                deliveryStatus = providerMsgId != null ? "SENT" : "FAILED";
            } else {
                log.warn("Cannot send WhatsApp — no phone number for entity: {}",
                        entity.getId());
            }
        }

        MessageLog msgLog = MessageLog.builder()
                .entityId(entity.getId())
                .channel(channel)
                .recipient(entity.getOwnerPhone() != null ? entity.getOwnerPhone() : "unknown")
                .templateId("notice-" + notice.getNoticeType().toLowerCase())
                .messageBody(messageText)
                .status(deliveryStatus)
                .providerMsgId(providerMsgId)
                .sentBy(actor)
                .build();

        messageLogRepo.save(msgLog);

        notice.setStatus("SENT");
        notice = noticeRepo.save(notice);

        // ✅ AUDIT
        auditService.log(actor, "SEND_NOTICE", "Notice", noticeId.toString());

        return notice;
    }

    @Transactional
    public Notice markServed(UUID noticeId, String actor) {

        Notice notice = findById(noticeId);
        notice.setServedAt(OffsetDateTime.now());
        notice.setStatus("SERVED");
        notice = noticeRepo.save(notice);

        // ✅ AUDIT
        auditService.log(actor, "MARK_SERVED", "Notice", noticeId.toString());

        return notice;
    }

    @Transactional
    public Notice updatePayment(UUID noticeId, String paymentStatus, String actor) {

        Notice notice = findById(noticeId);
        notice.setPaymentStatus(paymentStatus);
        notice = noticeRepo.save(notice);

        // ✅ AUDIT
        auditService.log(actor, "UPDATE_PAYMENT", "Notice", noticeId.toString());

        return notice;
    }

    public Page<Notice> list(String noticeType, String status,
                             String paymentStatus, Pageable pageable) {
        return noticeRepo.findByFilters(noticeType, status, paymentStatus, pageable);
    }

    public Notice findById(UUID id) {
        return noticeRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notice not found: " + id));
    }

    private String buildWhatsAppMessage(Notice notice, EntityMaster entity) {

        StringBuilder msg = new StringBuilder();

        msg.append("Dear ").append(safe(entity.getOwnerName())).append(",\n\n");

        msg.append(switch (notice.getNoticeType().toUpperCase()) {
            case "WARNING" -> "A *WARNING* notice has been issued for your establishment.";
            case "FINE" -> "A *FINE* notice has been issued for your establishment.";
            case "CLOSURE" -> "A *CLOSURE* notice has been issued for your establishment.";
            default -> "A notice has been issued for your establishment.";
        });

        msg.append("\n\n*Notice No:* ").append(notice.getNoticeNo());
        msg.append("\n*Establishment:* ").append(safe(entity.getName()));

        if (notice.getFineAmount() != null) {
            msg.append("\n*Fine Amount:* ").append(notice.getFineAmount()).append(" OMR");
        }

        if (notice.getStorageKeyPdf() != null) {
            msg.append("\n\n*View Notice PDF:*\n").append(notice.getStorageKeyPdf());
        }

        msg.append("\n\nFor queries please contact the Municipality.");

        return msg.toString();
    }

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

    private byte[] buildMinimalPdf(String noticeNo, EntityMaster entity, GenerateRequest req) {

        String content = "%PDF-1.4\n"
                + "1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n"
                + "2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n"
                + "3 0 obj<</Type/Page/MediaBox[0 0 595 842]/Parent 2 0 R/Contents 4 0 R>>endobj\n"
                + "4 0 obj<</Length 80>>stream\n"
                + "BT /F1 14 Tf 50 780 Td (Notice: " + noticeNo + ") Tj\n"
                + "0 -24 Td (Entity: " + safe(entity.getName()) + ") Tj\n"
                + "0 -24 Td (Type: " + req.noticeType() + ") Tj\n"
                + (req.fineAmount() != null ? "0 -24 Td (Fine: " + req.fineAmount() + ") Tj\n" : "")
                + "ET\nendstream endobj\n"
                + "xref\n0 5\ntrailer<</Size 5/Root 1 0 R>>\nstartxref\n0\n%%EOF";

        return content.getBytes();
    }

    private String safe(String s) {
        return s != null ? s.replace("(", "").replace(")", "") : "";
    }
}