package cn.court.mediation.entity;

import cn.court.mediation.enums.Channel;
import cn.court.mediation.enums.ScheduleStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 调解排期：每次排期/改期都保留一行，形成连续排期记录 */
@Getter
@Setter
@Entity
@Table(name = "mediation_schedule")
public class MediationSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long caseId;
    private Long mediatorId;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Channel channel;

    /** 线下地点 / 线上会议链接 */
    @Column(length = 300)
    private String location;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private ScheduleStatus status = ScheduleStatus.PLANNED;

    /** 第几次调解（从1开始） */
    private Integer roundNo = 1;

    /** 改期原因或备注 */
    @Column(length = 500)
    private String note;

    /** 申请人、被申请人是否确认出席 */
    private Boolean applicantConfirmed = false;
    private Boolean respondentConfirmed = false;

    private LocalDateTime createdAt = LocalDateTime.now();
}
