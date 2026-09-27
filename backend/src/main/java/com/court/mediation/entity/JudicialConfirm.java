package com.court.mediation.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 司法确认申请材料 */
@Data
@Entity
@Table(name = "judicial_confirm")
public class JudicialConfirm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @Column(name = "agreement_id")
    private Long agreementId;

    @Column(name = "confirm_no", unique = true, length = 40)
    private String confirmNo;

    /** 申请人/被申请人身份信息摘要 */
    @Column(name = "party_info", columnDefinition = "TEXT")
    private String partyInfo;

    /** 申请事项 */
    @Column(columnDefinition = "TEXT")
    private String claims;

    /** 随附材料清单 */
    @Column(name = "materials", columnDefinition = "TEXT")
    private String materials;

    /** DRAFT 材料生成 / SUBMITTED 已提交 / CONFIRMED 已裁定确认 / REJECTED 不予确认 */
    @Column(nullable = false, length = 20)
    private String status = "DRAFT";

    @Column(name = "ruling_no", length = 40)
    private String rulingNo;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
