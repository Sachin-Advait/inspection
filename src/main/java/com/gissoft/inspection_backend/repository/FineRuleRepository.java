package com.gissoft.inspection_backend.repository;

import com.gissoft.inspection_backend.entity.FineRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FineRuleRepository extends JpaRepository<FineRule, UUID> {
    Optional<FineRule> findByViolationCode(String violationCode);
}
