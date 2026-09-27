package com.court.mediation.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 协议付款计划/履行节点（支持分期；逾期由定时任务与手动扫描标记） */
@Data
@Entity
@Table(name = "payment_plan", indexes = @Index(name = "idx_pay_case", columnList = "case_id"))
public class PaymentPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @Column(name = "agreement_id")
    private Long agreementId;

    /** 期次，从 1 开始 */
    @Column(name = "period_no", nullable = false)
    private Integer periodNo;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    /** PENDING 待履行 / PAID 已履行 / OVERDUE 已逾期 / PARTIAL 部分履行（由 payment 实收决定） */
    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "paid_amount", precision = 14, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /** 收款确认人（执行联络人员） */
    @Column(name = "confirmed_by")
    private Long confirmedBy;

    @Column(length = 500)
    private String remark;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
