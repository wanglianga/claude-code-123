package cn.court.mediation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 履行回访 */
@Getter
@Setter
@Entity
@Table(name = "follow_up")
public class FollowUp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long caseId;
    private Long operatorId;
    @Column(length = 50)
    private String operatorName;

    private LocalDate followUpDate;

    /** NORMAL 履约正常 / PARTIAL 部分履行 / OVERDUE 逾期 / DONE 已履行完毕 / OBJECTION 有异议 */
    @Column(length = 15)
    private String result;

    @Column(length = 1000)
    private String note;

    /** 回访方式：PHONE 电话 / ONLINE 线上 / ONSITE 上门 */
    @Column(length = 10)
    private String way = "PHONE";

    private LocalDateTime createdAt = LocalDateTime.now();
}
