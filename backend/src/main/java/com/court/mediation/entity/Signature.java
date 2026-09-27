package com.court.mediation.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 线上签署记录（含人脸核验） */
@Data
@Entity
@Table(name = "agreement_signature")
public class Signature {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agreement_id", nullable = false)
    private Long agreementId;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @Column(name = "signer_user_id")
    private Long signerUserId;

    /** APPLICANT 申请人 / RESPONDENT 被申请人 / MEDIATOR 调解员 */
    @Column(name = "signer_role", length = 20)
    private String signerRole;

    @Column(name = "signer_name", length = 50)
    private String signerName;

    /** 人脸核验是否通过 */
    @Column(name = "face_verified")
    private Boolean faceVerified = false;

    /** 人脸核验置信度（演示） */
    @Column(name = "face_score")
    private Double faceScore;

    /** 电子签名图片/签章数据（演示存 base64 缩略信息） */
    @Column(name = "sign_data", length = 1000)
    private String signData;

    @Column(name = "client_ip", length = 50)
    private String clientIp;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @PrePersist
    public void prePersist() {
        if (signedAt == null) signedAt = LocalDateTime.now();
    }
}
