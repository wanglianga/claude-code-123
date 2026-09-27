package cn.court.mediation.repository;

import cn.court.mediation.entity.MediationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MediationRecordRepository extends JpaRepository<MediationRecord, Long> {
    List<MediationRecord> findByCaseIdOrderByRoundNoDescCreatedAtDesc(Long caseId);
}
