package com.court.mediation.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 案件时间线：围绕同一案件，窗口、调解员、当事人、法官助理、执行联络的每一步动作
 * 均落一条记录，形成可提交法官的连续过程材料。
 */
@Data
@Entity
@Table(name = "case_timeline", indexes = @Index(name = "idx_timeline_case", columnList = "case_id"))
public class TimelineEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    /**
     * 事件类型：CREATED 提交立案 / TRIAGED 分流 / SCHEDULED 排期 / RESCHEDULED 改期 /
     * SESSION_HELD 召开调解 / UNREACHABLE 失联登记 / REACHED 恢复联系 /
     * EVIDENCE_MISSING 缺料 / EVIDENCE_SUPPLEMENTED 补证 / MEDIATION_NOTE 调解记录 /
     * AGREEMENT_DRAFTED 协议起草 / SIGNED 签署 / FACE_VERIFIED 人脸核验 /
     * SERVED 电子送达 / PAYMENT_DUE 付款节点 / PAYMENT_PAID 收款确认 /
     * PAYMENT_OVERDUE 逾期 / REMIND 催办 / VISIT 回访 /
     * TRANSFER_REQUESTED 申请转立案 / TRANSFERRED 转立案 /
     * JUDICIAL_APPLY 申请司法确认 / JUDICIAL_CONFIRMED 司法确认 / STATUS_CHANGED 状态变更
     */
    @Column(nullable = false, length = 40)
    private String type;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "operator_name", length = 50)
    private String operatorName;

    @Column(name = "operator_role", length = 30)
    private String operatorRole;

    /** 关联业务单据 id（如协议、排期），可空 */
    @Column(name = "ref_id")
    private Long refId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
