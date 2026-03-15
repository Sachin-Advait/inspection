package com.gissoft.inspection_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "message_log")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MessageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    /** WHATSAPP | SMS */
    @Column(nullable = false, length = 20)
    private String channel;

    @Column(nullable = false, length = 80)
    private String recipient;

    @Column(name = "template_id", length = 80)
    private String templateId;

    @Column(name = "message_body", length = 2000)
    private String messageBody;

    /** SENT | DELIVERED | READ | FAILED */
    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "provider_msg_id", length = 120)
    private String providerMsgId;

    @Column(name = "sent_by", length = 80)
    private String sentBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
