package com.gissoft.inspection_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "checklist_question")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChecklistQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    @JsonIgnoreProperties({"questions", "template", "hibernateLazyInitializer", "handler"})
    private ChecklistSection section;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false, length = 400)
    private String text;

    /**
     * PASS_FAIL | YES_NO | TEXT | NUMBER | CHOICE
     */
    @Column(name = "answer_type", nullable = false, length = 30)
    private String answerType;

    @Column(nullable = false)
    private boolean required = true;

    @Type(JsonBinaryType.class)
    @Column(name = "validations_json", columnDefinition = "jsonb")
    private Map<String, Object> validationsJson;

    @OneToOne(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonIgnoreProperties({"question"})
    private ChecklistRule rule;
}