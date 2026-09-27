package cn.court.mediation.repository;

import cn.court.mediation.entity.MediationAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgreementRepository extends JpaRepository<MediationAgreement, Long> {
    Optional<MediationAgreement> findByCaseId(Long caseId);
}
