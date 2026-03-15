//package com.gissoft.inspection_backend.entity;
//
//import jakarta.persistence.Entity;
//import jakarta.persistence.ManyToOne;
//import jakarta.persistence.Table;
//import lombok.Getter;
//import lombok.Setter;
//
//import java.time.Instant;
//
//@Entity
//@Table(name = "enforcement_notice")
//@Getter
//@Setter
//public class EnforcementNotice extends BaseEntity {
//
//    @ManyToOne
//    private InspectionViolation violation;
//
//    private String noticeNumber;
//
//    private String noticeType;
//
//    private Long fineAmount;
//
//    private String status;
//
//    private Instant issuedAt;
//
//    private Instant dueDate;
//}
