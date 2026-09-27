package com.court.mediation.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 调解协议 */
@Data
@Entity
@Table(name = "mediation_agreement")
public class Agreement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @Column(name = "agreement_no", unique = true, length = 40)
    private String agreementNo;

    /** 协议正文 */
    @Column(columnDefinition = "TEXT")
    private String content;

    /** 调解总金额 */
    @Column(name = "total_amount", precision = 14, scale = 2)
    private BigDecimal totalAmount;

    /** 履行方式：LUMP_SUM 一次性 / INSTALLMENTS 分期 */
    @Column(name = "payment_method", length = 20)
    private String paymentMethod;

    /** 担保人 */
    @Column(length = 100)
    private String guarantor;

    /** 履行期限截止 */
    @Column(name = "deadline")
    private LocalDate deadline;

    /** 是否申请司法确认 */
    @Column(name = "apply_judicial_confirm")
    private Boolean applyJudicialConfirm = false;

    /** DRAFT 草稿 / SIGNED 已签署 / VOID 作废 */
    @Column(nullable = false, length = 20)
    private String status = "DRAFT";

    @Column(name = "mediator_id")
    private Long mediatorId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
