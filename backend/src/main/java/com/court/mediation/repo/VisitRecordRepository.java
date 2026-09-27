package com.court.mediation.repo;

import com.court.mediation.entity.VisitRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VisitRecordRepository extends JpaRepository<VisitRecord, Long> {
    List<VisitRecord> findByCaseIdOrderByVisitTimeDesc(Long caseId);
}
