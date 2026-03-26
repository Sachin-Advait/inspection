package com.gissoft.inspection_backend.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.gissoft.inspection_backend.entity.EvidenceFile;
import com.gissoft.inspection_backend.repository.EvidenceFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EvidenceService {

    private final EvidenceFileRepository evidenceRepo;
    private final Cloudinary cloudinary;
    private final AuditService auditService;

    // ── Upload ────────────────────────────────────────────────────────────────

    public EvidenceFile upload(UUID inspectionId,
                               UUID entityId,
                               UUID questionId,
                               MultipartFile file,
                               String capturedBy) throws Exception {

        byte[] bytes = file.getBytes();
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
        String sha256 = HexFormat.of().formatHex(digest);

        String publicId = "evidence/" + entityId + "/" + inspectionId + "/" + UUID.randomUUID();

        Map uploadResult = cloudinary.uploader().upload(
                bytes,
                ObjectUtils.asMap(
                        "public_id", publicId,
                        "resource_type", "auto"
                )
        );

        String fileUrl = uploadResult.get("secure_url").toString();

        EvidenceFile ev = EvidenceFile.builder()
                .inspectionId(inspectionId)
                .entityId(entityId)
                .questionId(questionId)
                .fileType(detectType(file.getContentType()))
                .storageKey(fileUrl)
                .originalFilename(file.getOriginalFilename())
                .mimeType(file.getContentType())
                .fileSizeBytes(file.getSize())
                .sha256Hash(sha256)
                .capturedBy(capturedBy)
                .capturedAt(OffsetDateTime.now())
                .build();

        ev = evidenceRepo.save(ev);

        // ✅ CLEAN AUDIT
        auditService.log(
                capturedBy,
                "UPLOAD_EVIDENCE",
                "EvidenceFile",
                ev.getId().toString()
        );

        return ev;
    }

    // ── File URL ─────────────────────────────────────────────────────────────

    public String getFileUrl(UUID evidenceId) {

        EvidenceFile ev = evidenceRepo.findById(evidenceId)
                .orElseThrow(() -> new IllegalArgumentException("Evidence not found: " + evidenceId));

        return ev.getStorageKey();
    }

    // ── Browse ───────────────────────────────────────────────────────────────

    public Page<EvidenceFile> browse(UUID entityId, String fileType, Pageable pageable) {
        return evidenceRepo.findByFilters(entityId, fileType, pageable);
    }

    public List<EvidenceFile> byInspection(UUID inspectionId) {
        return evidenceRepo.findByInspectionId(inspectionId);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String detectType(String mime) {

        if (mime == null) return "PHOTO";

        if (mime.startsWith("image/")) return "PHOTO";
        if (mime.startsWith("video/")) return "VIDEO";
        if ("application/pdf".equals(mime)) return "PDF";

        return "PHOTO";
    }
}