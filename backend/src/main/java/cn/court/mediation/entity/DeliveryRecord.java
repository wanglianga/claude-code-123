package cn.court.mediation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 电子送达记录 */
@Getter
@Setter
@Entity
@Table(name = "delivery_record")
public class DeliveryRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long caseId;

    /** 送达对象：APPLICANT / RESPONDENT */
    @Column(length = 15)
    private String target;
    @Column(length = 50)
    private String targetName;
    @Column(length = 20)
    private String targetPhone;

    /** 送达文书：排期通知、调解协议、司法确认裁定等 */
    @Column(length = 100)
    private String documentName;

    /** 送达方式：SMS 短信 / EMAIL 邮件 / ONLINE 站内 */
    @Column(length = 10)
    private String channel = "SMS";

    /** SENT 已送达 / READ 已读签 / FAILED 送达失败 */
    @Column(length = 10)
    private String status = "SENT";

    private LocalDateTime sentAt = LocalDateTime.now();
    private LocalDateTime readAt;

    @Column(length = 300)
    private String detail;
}
