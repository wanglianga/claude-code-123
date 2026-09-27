package com.court.mediation.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 批量导入批次（CSV/Excel 批量案件导入） */
@Data
@Entity
@Table(name = "import_batch")
public class ImportBatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", length = 255)
    private String fileName;

    /** 总行数 */
    @Column(name = "total_count")
    private Integer totalCount = 0;

    @Column(name = "success_count")
    private Integer successCount = 0;

    @Column(name = "fail_count")
    private Integer failCount = 0;

    /** 失败明细，每行一条 */
    @Column(name = "fail_detail", columnDefinition = "TEXT")
    private String failDetail;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
