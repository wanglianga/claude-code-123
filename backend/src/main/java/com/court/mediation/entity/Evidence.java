package com.court.mediation.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 证据材料清单条目 */
@Data
@Entity
@Table(name = "case_evidence")
public class Evidence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    /** 证据名称，如：借条、转账凭证、物业合同、事故认定书 */
    @Column(nullable = false, length = 200)
    private String name;

    /** 证据类型：书证 / 物证 / 电子数据 / 视听资料 / 证人证言 */
    @Column(length = 30)
    private String evidenceType;

    @Column(length = 255)
    private String fileName;

    @Column(length = 500)
    private String remark;

    /** 是否经质证核验 */
    @Column(name = "verified")
    private Boolean verified = false;

    @Column(name = "submitted_by")
    private Long submittedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
