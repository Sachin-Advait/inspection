//package com.gissoft.inspection_backend.entity;
//
//import jakarta.persistence.*;
//import lombok.Getter;
//import lombok.Setter;
//import java.time.Instant;
//import java.util.UUID;
//
//@MappedSuperclass
//@Getter
//@Setter
//public abstract class BaseEntity {
//
//    @Id
//    @GeneratedValue
//    private UUID id;
//
//    @Column(nullable = false)
//    private Instant createdAt = Instant.now();
//}