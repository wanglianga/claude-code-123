package com.court.mediation.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 电子送达记录 */
@Data
@Entity
@Table(name = "service_record")
public class ServiceRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    /** 送达文书：调解通知 / 排期通知 / 协议书 / 履行催告 / 转立案告知 */
    @Column(nullable = false, length = 100)
    private String documentName;

    /** 受送达人 */
    @Column(name = "receiver_name", length = 50)
    private String receiverName;

    @Column(name = "receiver_phone", length = 20)
    private String receiverPhone;

    /** SMS 短信 / ONLINE 平台站内 / EMAIL 邮件 */
    @Column(length = 20)
    private String channel = "ONLINE";

    /** PENDING 已发起 / DELIVERED 已送达 / READ 已读 / FAILED 送达失败 */
    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(length = 500)
    private String content;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
