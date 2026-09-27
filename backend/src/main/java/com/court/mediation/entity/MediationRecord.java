package com.court.mediation.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 调解过程记录：争议焦点、双方让步、付款计划、担保人、履行期限、司法确认意向 */
@Data
@Entity
@Table(name = "mediation_record")
public class MediationRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @Column(name = "mediator_id")
    private Long mediatorId;

    @Column(name = "session_time")
    private LocalDateTime sessionTime;

    /** 参会人员 */
    @Column(length = 500)
    private String participants;

    /** 争议焦点 */
    @Column(name = "focus_issues", columnDefinition = "TEXT")
    private String focusIssues;

    /** 双方让步情况 */
    @Column(columnDefinition = "TEXT")
    private String concessions;

    /** 付款计划（口头/文字方案，正式分期以 payment_plan 表为准） */
    @Column(name = "payment_plan", columnDefinition = "TEXT")
    private String paymentPlan;

    /** 担保人 */
    @Column(length = 100)
    private String guarantor;

    /** 履行期限 */
    @Column(name = "performance_deadline")
    private LocalDate performanceDeadline;

    /** 是否申请司法确认 */
    @Column(name = "apply_judicial_confirm")
    private Boolean applyJudicialConfirm = false;

    /** AGREED 达成一致 / NO_AGREEMENT 未达成 / PENDING 继续调解 */
    @Column(length = 20)
    private String result = "PENDING";

    @Column(columnDefinition = "TEXT")
    private String remark;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
