package com.gissoft.inspection_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "checklist_section")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChecklistSection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    @JsonIgnoreProperties({"sections", "hibernateLazyInitializer", "handler"})
    private ChecklistTemplate template;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 400)
    private String description;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    @JsonIgnoreProperties({"section"})
    private List<ChecklistQuestion> questions = new ArrayList<>();
}