package com.gissoft.inspection_backend.services;

import com.gissoft.inspection_backend.repository.ApprovalRequestRepository;
import com.gissoft.inspection_backend.repository.InspectionRunRepository;
import com.gissoft.inspection_backend.repository.NoticeRepository;
import com.gissoft.inspection_backend.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final InspectionRunRepository inspectionRunRepo;
    private final ApprovalRequestRepository approvalRepo;
    private final NoticeRepository noticeRepo;
    private final TaskRepository taskRepo;
    // OracleOutboxEventRepository removed — Oracle integration not active yet

    public DashboardStats getStats(String dg, OffsetDateTime from, OffsetDateTime to) {
        OffsetDateTime todayStart = OffsetDateTime.now()
                .withHour(0).withMinute(0).withSecond(0).withNano(0);

        long inspectionsToday = inspectionRunRepo.countSince(todayStart);
        long pendingApprovals = approvalRepo.countByStatus("PENDING");
        long overdueTasks = taskRepo.countByAssignedToAndStatusAndDueAtBefore(
                null, "PENDING", OffsetDateTime.now());
        long finesIssued = noticeRepo.count();
        long finesPaid = noticeRepo.countByPaymentStatus("PAID");
        long finesUnpaid = noticeRepo.countByPaymentStatus("UNPAID");

        Map<String, Object> health = new LinkedHashMap<>();
        health.put("whatsapp", "enabled");
        health.put("cloudinary", "enabled");
        health.put("oracle", "demo-mode");   // swap to real stats when Oracle is live

        return new DashboardStats(inspectionsToday, pendingApprovals, overdueTasks,
                finesIssued, finesPaid, finesUnpaid, health);
    }

    public record DashboardStats(
            long inspectionsToday,
            long pendingApprovals,
            long overdueTasks,
            long finesIssued,
            long finesPaid,
            long finesUnpaid,
            Map<String, Object> integrationHealth
    ) {
    }
}