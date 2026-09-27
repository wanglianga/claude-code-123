package cn.court.mediation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 案件时间线事件：所有角色在同一案件上的关键动作统一落表，
 * 保证多次改期、失联、部分履行等过程材料连续，可整体提交法官。
 */
@Getter
@Setter
@Entity
@Table(name = "case_timeline", indexes = @Index(name = "idx_timeline_case", columnList = "caseId"))
public class TimelineEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long caseId;

    /** 事件类型：SUBMIT/TRIAGE/SCHEDULE/RESCHEDULE/RECORD/AGREEMENT/SIGN/FACE/PERFORMANCE/OVERDUE/FOLLOWUP/TRANSFER/DELIVERY/MISSING/EVIDENCE 等 */
    @Column(length = 30)
    private String type;

    @Column(length = 200)
    private String title;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String detail;

    /** 操作人 */
    private Long operatorId;
    @Column(length = 50)
    private String operatorName;
    @Column(length = 20)
    private String operatorRole;

    /** 关联材料文件名（如导出的司法确认申请书） */
    @Column(length = 200)
    private String attachmentName;

    private LocalDateTime createdAt = LocalDateTime.now();
}
