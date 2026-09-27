package cn.court.mediation.repository;

import cn.court.mediation.entity.FaceVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FaceVerificationRepository extends JpaRepository<FaceVerification, Long> {
    List<FaceVerification> findByCaseIdOrderByCreatedAtDesc(Long caseId);
}
