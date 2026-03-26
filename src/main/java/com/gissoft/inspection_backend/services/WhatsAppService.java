package com.gissoft.inspection_backend.services;

import com.gissoft.inspection_backend.entity.MessageLog;
import com.gissoft.inspection_backend.repository.MessageLogRepository;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Log4j2
@Service
@AllArgsConstructor
public class WhatsAppService {

    private final MessageLogRepository logRepo;
    private final AuditService auditService;

    private static final String ACCOUNT_SID = "AC31f111ff92893d6319a3e96fa4bed3f6";
    private static final String AUTH_TOKEN = "0606ff938e33d9c13a6e5e5206a28ade";
    private static final String FROM = "whatsapp:+14155238886";

    @PostConstruct
    public void init() {
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
        log.info("Twilio initialized successfully");
    }

    public String sendMessage(String to, String messageText, String actor) {

        MessageLog log = MessageLog.builder()
                .entityId(UUID.randomUUID())
                .channel("WHATSAPP")
                .recipient(to)
                .messageBody(messageText)
                .status("SENT")
                .sentBy(actor)
                .build();

        try {
            Message message = Message.creator(
                    new PhoneNumber("whatsapp:" + to),
                    new PhoneNumber(FROM),
                    messageText
            ).create();

            log.setProviderMsgId(message.getSid());
            log.setStatus("DELIVERED");

        } catch (Exception e) {
            log.setStatus("FAILED");
        }

        log.setCreatedAt(OffsetDateTime.now());
        logRepo.save(log);

        // ✅ AUDIT
        auditService.log(actor, "SEND_WHATSAPP", "MessageLog", log.getId().toString(),
                Map.of("recipient", to, "status", log.getStatus()));

        return log.getProviderMsgId();
    }
}