package com.court.mediation.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 调解排期（改期采用版本化：旧记录置 CANCELLED，新插入一条） */
@Data
@Entity
@Table(name = "mediation_schedule")
public class Schedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @Column(name = "mediator_id")
    private Long mediatorId;

    /** ONLINE 线上调解 / OFFLINE 线下调解 */
    @Column(length = 10)
    private String mode;

    @Column(name = "scheduled_start", nullable = false)
    private LocalDateTime scheduledStart;

    @Column(name = "scheduled_end")
    private LocalDateTime scheduledEnd;

    /** 线下地点或线上会议链接 */
    @Column(length = 255)
    private String location;

    /** PROPOSED 待确认 / CONFIRMED 已确认 / COMPLETED 已完成 / CANCELLED 已取消(改期) */
    @Column(nullable = false, length = 20)
    private String status = "PROPOSED";

    @Column(name = "version")
    private Integer version = 1;

    /** 当事人是否确认可用时间 */
    @Column(name = "party_confirmed")
    private Boolean partyConfirmed = false;

    @Column(length = 500)
    private String note;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
