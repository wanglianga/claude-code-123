package cn.court.mediation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 转立案材料包：由调解全卷连续材料生成，避免当事人在调解与立案窗口间反复补材料 */
@Getter
@Setter
@Entity
@Table(name = "transfer_material")
public class TransferMaterial {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Long caseId;

    /** 转立案原因分类：MISSING 失联 / EVIDENCE 证据不足 / REJECT 拒绝调解 / OVERDUE 协议逾期 / OTHER */
    @Column(length = 15)
    private String reasonType;

    @Column(length = 500)
    private String reasonDetail;

    /** 材料清单（由时间线、笔录、协议、履行记录自动汇总），换行分隔 */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String materialList;

    /** 汇编正文（纯文本导出用） */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String compiledContent;

    /** PREPARED 已生成 / ACCEPTED 立案窗口已接收 */
    @Column(length = 10)
    private String status = "PREPARED";

    private Long preparedBy;
    private LocalDateTime preparedAt = LocalDateTime.now();
    private LocalDateTime acceptedAt;
}
