package com.court.mediation.repo;

import com.court.mediation.entity.ServiceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceRecordRepository extends JpaRepository<ServiceRecord, Long> {
    List<ServiceRecord> findByCaseIdOrderByCreatedAtDesc(Long caseId);
}
