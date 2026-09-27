package com.court.mediation.repo;

import com.court.mediation.entity.CaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface CaseRepository extends JpaRepository<CaseEntity, Long>, JpaSpecificationExecutor<CaseEntity> {
    Optional<CaseEntity> findByCaseNo(String caseNo);

    List<CaseEntity> findByStatusOrderByUrgencyDescCreatedAtDesc(String status);

    long countByStatus(String status);
}
