package com.gissoft.inspection_backend.repository;

import com.gissoft.inspection_backend.entity.ChecklistSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChecklistSectionRepository extends JpaRepository<ChecklistSection, UUID> {
    List<ChecklistSection> findByTemplateIdOrderBySortOrderAsc(UUID templateId);
}
