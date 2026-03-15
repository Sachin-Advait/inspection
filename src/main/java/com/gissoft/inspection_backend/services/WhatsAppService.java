package com.gissoft.inspection_backend.services;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class WhatsAppService {

    // Direct credentials (hardcoded)
    private static final String ACCOUNT_SID = "AC31f111ff92893d6319a3e96fa4bed3f6";
    private static final String AUTH_TOKEN = "0606ff938e33d9c13a6e5e5206a28ade";
    private static final String FROM = "whatsapp:+14155238886";

    @PostConstruct
    public void init() {
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
        log.info("Twilio initialized successfully");
    }

    public String sendMessage(String to, String messageText) {
        try {
            log.info("Sending WhatsApp message to: {}", to);

            Message message = Message.creator(
                    new PhoneNumber("whatsapp:" + to),
                    new PhoneNumber(FROM),
                    messageText
            ).create();

            log.info("WhatsApp message sent successfully. SID: {}", message.getSid());
            return message.getSid();

        } catch (Exception e) {
            log.error("Failed to send WhatsApp message to {}: {}", to, e.getMessage(), e);
            return null;
        }
    }
}