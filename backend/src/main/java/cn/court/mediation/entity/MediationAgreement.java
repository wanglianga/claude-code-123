package cn.court.mediation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 调解协议 */
@Getter
@Setter
@Entity
@Table(name = "mediation_agreement")
public class MediationAgreement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Long caseId;

    /** 协议总金额 */
    @Column(precision = 14, scale = 2)
    private BigDecimal totalAmount;

    /** 是否分期履行 */
    private Boolean installment = false;

    /** 付款计划摘要，如 "分3期：2026-10-10付10000..." */
    @Column(length = 1000)
    private String paymentPlan;

    /** 履行期限（最后一期/一次性付清截止日） */
    private LocalDate deadline;

    /** 担保人姓名 */
    @Column(length = 50)
    private String guarantorName;
    @Column(length = 20)
    private String guarantorPhone;
    @Column(length = 200)
    private String guarantorNote;

    /** 是否申请司法确认 */
    private Boolean judicialConfirmRequested = false;

    /** 协议正文 */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;

    /** 申请人是否线上签署 */
    private Boolean applicantSigned = false;
    private LocalDateTime applicantSignedAt;
    private Boolean respondentSigned = false;
    private LocalDateTime respondentSignedAt;

    /** 签署时人脸核验是否通过 */
    private Boolean faceVerified = false;
    private LocalDateTime faceVerifiedAt;

    /** 全部签署完成时间（协议生效时间） */
    private LocalDateTime effectiveAt;

    private LocalDateTime createdAt = LocalDateTime.now();
}
