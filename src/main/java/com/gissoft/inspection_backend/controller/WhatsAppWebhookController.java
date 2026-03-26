package com.gissoft.inspection_backend.controller;

import com.gissoft.inspection_backend.entity.MessageLog;
import com.gissoft.inspection_backend.repository.MessageLogRepository;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/webhook")
@AllArgsConstructor
public class WhatsAppWebhookController {

    private final MessageLogRepository repo;

    @PostMapping("/whatsapp-status")
    public void updateStatus(
            @RequestParam("MessageSid") String sid,
            @RequestParam("MessageStatus") String status
    ) {

        Optional<MessageLog> optionalLog = repo.findByProviderMsgId(sid);

        if (optionalLog.isPresent()) {
            MessageLog log = optionalLog.get();

            if ("delivered".equalsIgnoreCase(status)) {
                log.setStatus("DELIVERED");
            } else if ("read".equalsIgnoreCase(status)) {
                log.setStatus("READ");
            } else if ("failed".equalsIgnoreCase(status)) {
                log.setStatus("FAILED");
            }

            repo.save(log);
        }
    }
}