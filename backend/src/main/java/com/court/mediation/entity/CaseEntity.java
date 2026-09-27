package com.court.mediation.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 诉前调解案件（贯穿分流-排期-调解-协议-履行-转立案全生命周期）
 */
@Data
@Entity
@Table(name = "mediation_case", indexes = {
        @Index(name = "idx_case_status", columnList = "status"),
        @Index(name = "idx_case_cause", columnList = "cause_type"),
        @Index(name = "idx_case_mediator", columnList = "mediator_id")
})
public class CaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 诉前调字号业务编号 */
    @Column(name = "case_no", unique = true, length = 40)
    private String caseNo;

    @Column(nullable = false, length = 200)
    private String title;

    /** 案由：民间借贷 / 物业费 / 交通事故 / 邻里纠纷 / 小额买卖合同 */
    @Column(name = "cause_type", nullable = false, length = 30)
    private String causeType;

    /** 诉求金额（元） */
    @Column(name = "claim_amount", precision = 14, scale = 2)
    private BigDecimal claimAmount;

    @Column(name = "applicant_name", length = 50)
    private String applicantName;

    @Column(name = "applicant_phone", length = 20)
    private String applicantPhone;

    @Column(name = "applicant_user_id")
    private Long applicantUserId;

    @Column(name = "respondent_name", length = 50)
    private String respondentName;

    @Column(name = "respondent_phone", length = 20)
    private String respondentPhone;

    @Column(name = "respondent_address", length = 255)
    private String respondentAddress;

    @Column(name = "respondent_user_id")
    private Long respondentUserId;

    /** 证据清单（手工录入文本，明细可在 evidence 表维护） */
    @Column(name = "evidence_summary", columnDefinition = "TEXT")
    private String evidenceSummary;

    /** 纠纷情况说明 */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** 是否愿意线上调解 */
    @Column(name = "willing_online")
    private Boolean willingOnline = true;

    /** 紧急程度：NORMAL 普通 / URGENT 紧急 / VERY_URGENT 特急 */
    @Column(name = "urgency", length = 20)
    private String urgency = "NORMAL";

    /** 是否涉及外地当事人 */
    @Column(name = "remote_party")
    private Boolean remoteParty = false;

    /**
     * 状态：PENDING_TRIAGE 待分流 / PENDING_SCHEDULE 已分流待排期 / SCHEDULED 已排期 /
     * MEDIATING 调解中 / AGREED 已达成协议 / PERFORMING 履行中 / COMPLETED 已履行完毕 /
     * FAILED 调解不成 / TRANSFERRED 已转立案
     */
    @Column(nullable = false, length = 30)
    private String status = "PENDING_TRIAGE";

    @Column(name = "mediator_id")
    private Long mediatorId;

    /** 调解方式：ONLINE 线上 / OFFLINE 线下 */
    @Column(name = "mediation_mode", length = 10)
    private String mediationMode;

    @Column(name = "triage_user_id")
    private Long triageUserId;

    @Column(name = "triage_remark", length = 500)
    private String triageRemark;

    /** 被申请人失联标记（来自失联登记，连续过程材料关键字段） */
    @Column(name = "respondent_unreachable")
    private Boolean respondentUnreachable = false;

    /** 证据材料缺失标记 */
    @Column(name = "evidence_missing")
    private Boolean evidenceMissing = false;

    /** 改期次数（多次改期作为转立案裁量依据） */
    @Column(name = "reschedule_count")
    private Integer rescheduleCount = 0;

    @Column(name = "import_batch_id")
    private Long importBatchId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
