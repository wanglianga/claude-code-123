package com.court.mediation.repo;

import com.court.mediation.entity.ImportBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportBatchRepository extends JpaRepository<ImportBatch, Long> {
    List<ImportBatch> findAllByOrderByCreatedAtDesc();
}
