package com.court.mediation.repo;

import com.court.mediation.entity.MediationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MediationRecordRepository extends JpaRepository<MediationRecord, Long> {
    List<MediationRecord> findByCaseIdOrderBySessionTimeAsc(Long caseId);
}
