package com.gissoft.inspection_backend.dto;

import lombok.Data;

@Data
public class WhatsAppRequest {
    private String to;
    private String msg;
}
