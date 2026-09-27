package com.court.mediation.repo;

import com.court.mediation.entity.Agreement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgreementRepository extends JpaRepository<Agreement, Long> {
    Optional<Agreement> findByCaseIdAndStatus(Long caseId, String status);
    List<Agreement> findByCaseIdOrderByCreatedAtDesc(Long caseId);
}
