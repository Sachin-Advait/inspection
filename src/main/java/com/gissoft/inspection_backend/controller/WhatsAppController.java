package com.gissoft.inspection_backend.controller;

import com.gissoft.inspection_backend.dto.WhatsAppRequest;
import com.gissoft.inspection_backend.entity.MessageLog;
import com.gissoft.inspection_backend.repository.MessageLogRepository;
import com.gissoft.inspection_backend.services.WhatsAppService;
import org.springframework.data.domain.Page;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/whatsapp")
@AllArgsConstructor
public class WhatsAppController {

    private final WhatsAppService service;
    private final MessageLogRepository repo;

    @PostMapping("/send")
    public String send(@RequestBody WhatsAppRequest request,
                       Principal principal) {
        return service.sendMessage(
                request.getTo(),
                request.getMsg(),
                principal.getName()
        );
    }

    @GetMapping("/message-logs")
    public Page<MessageLog> getLogs(
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String status,
            @RequestParam OffsetDateTime from,
            @RequestParam OffsetDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return repo.findByFilters(
                channel,
                status,
                from,
                to,
                PageRequest.of(page, size)
        );
    }
}