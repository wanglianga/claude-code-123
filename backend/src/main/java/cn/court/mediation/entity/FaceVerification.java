package cn.court.mediation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 人脸核验记录（演示环境做模拟核验：姓名+身份证号后四位比对） */
@Getter
@Setter
@Entity
@Table(name = "face_verification")
public class FaceVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long caseId;

    /** APPLICANT / RESPONDENT / GUARANTOR */
    @Column(length = 15)
    private String party;
    @Column(length = 50)
    private String partyName;

    /** 证件号后四位（演示采集） */
    @Column(length = 10)
    private String idCardTail;

    /** 核验得分（模拟 80-100） */
    private Integer score;

    private Boolean passed = false;

    @Column(length = 300)
    private String note;

    private LocalDateTime createdAt = LocalDateTime.now();
}
