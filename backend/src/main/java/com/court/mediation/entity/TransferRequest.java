package com.court.mediation.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 转立案申请与材料（一方申请转立案 / 调解不成转立案） */
@Data
@Entity
@Table(name = "transfer_request")
public class TransferRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    /** 申请人角色/名称 */
    @Column(name = "applicant_name", length = 50)
    private String applicantName;

    /** 转立案原因：被申请人失联 / 证据材料缺失 / 多次改期 / 逾期未履行 / 调解不成 / 当事人申请 */
    @Column(name = "reason_code", length = 50)
    private String reasonCode;

    @Column(columnDefinition = "TEXT")
    private String reason;

    /** 随附连续过程材料清单（系统自动汇总：时间线、调解记录、协议、送达、付款、回访等） */
    @Column(name = "material_summary", columnDefinition = "TEXT")
    private String materialSummary;

    /** SUBMITTED 已申请 / ACCEPTED 已接收立案 / REJECTED 不予转立案 */
    @Column(nullable = false, length = 20)
    private String status = "SUBMITTED";

    /** 立案后案号 */
    @Column(name = "filing_no", length = 40)
    private String filingNo;

    @Column(name = "reviewer_id")
    private Long reviewerId;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
