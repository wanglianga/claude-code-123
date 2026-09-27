package cn.court.mediation.repository;

import cn.court.mediation.entity.DeliveryRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryRepository extends JpaRepository<DeliveryRecord, Long> {
    List<DeliveryRecord> findByCaseIdOrderBySentAtDesc(Long caseId);
}
