package com.gissoft.inspection_backend.controller;

import com.gissoft.inspection_backend.entity.MessageLog;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessagingController {

    /**
     * GET /api/messages/logs?from=&to=&channel=WHATSAPP&status=FAILED
     */
    @GetMapping("/logs")
    public ResponseEntity<Page<MessageLog>> getMessageLogs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        // TODO: Inject MessageLogRepository and implement
        return ResponseEntity.ok(Page.empty());
    }
}