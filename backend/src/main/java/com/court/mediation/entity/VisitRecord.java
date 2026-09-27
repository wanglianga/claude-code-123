package com.court.mediation.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 履行回访记录 */
@Data
@Entity
@Table(name = "visit_record")
public class VisitRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @Column(name = "agreement_id")
    private Long agreementId;

    @Column(name = "visit_time")
    private LocalDateTime visitTime;

    /** PHONE 电话 / ONLINE 线上 / ONSITE 上门 */
    @Column(length = 20)
    private String channel = "PHONE";

    /** 回访对象：申请人 / 被申请人 / 双方 */
    @Column(length = 30)
    private String target;

    /** PERFORMED 已按约履行 / PARTIAL 部分履行 / BREACH 未履行 / OBJECTION 提出异议 */
    @Column(length = 20)
    private String result;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "visitor_id")
    private Long visitorId;

    @Column(name = "visitor_name", length = 50)
    private String visitorName;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (visitTime == null) visitTime = LocalDateTime.now();
    }
}
