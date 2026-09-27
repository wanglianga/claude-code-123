package cn.court.mediation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 调解笔录/过程记录：争议焦点、双方让步、协商过程等 */
@Getter
@Setter
@Entity
@Table(name = "mediation_record")
public class MediationRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long caseId;
    private Long scheduleId;
    private Long mediatorId;

    private Integer roundNo = 1;

    /** 争议焦点 */
    @Column(length = 1000)
    private String focusPoints;

    /** 申请人让步 */
    @Column(length = 1000)
    private String applicantConcession;

    /** 被申请人让步 */
    @Column(length = 1000)
    private String respondentConcession;

    /** 协商过程/调解意见 */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String processNote;

    /** 本次调解结果：SUCCESS 达成协议 / FAIL 未达成 / CONTINUE 继续调解 */
    @Column(length = 15)
    private String outcome;

    private LocalDateTime createdAt = LocalDateTime.now();
}
