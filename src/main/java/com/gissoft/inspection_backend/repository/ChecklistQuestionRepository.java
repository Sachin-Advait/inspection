package com.gissoft.inspection_backend.repository;

import com.gissoft.inspection_backend.entity.ChecklistQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChecklistQuestionRepository extends JpaRepository<ChecklistQuestion, UUID> {
    List<ChecklistQuestion> findBySectionIdOrderBySortOrderAsc(UUID sectionId);
}
