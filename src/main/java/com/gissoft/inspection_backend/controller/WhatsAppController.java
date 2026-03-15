package com.gissoft.inspection_backend.controller;


import com.gissoft.inspection_backend.dto.WhatsAppRequest;
import com.gissoft.inspection_backend.services.WhatsAppService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/whatsapp")
@AllArgsConstructor
public class WhatsAppController {

    private final WhatsAppService service;

    @PostMapping("/send")
    public String send(@RequestBody WhatsAppRequest request) {
        return service.sendMessage(request.getTo(), request.getMsg());
    }
}

