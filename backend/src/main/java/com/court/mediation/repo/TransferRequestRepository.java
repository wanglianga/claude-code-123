package com.court.mediation.repo;

import com.court.mediation.entity.TransferRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransferRequestRepository extends JpaRepository<TransferRequest, Long> {
    List<TransferRequest> findByCaseIdOrderByCreatedAtDesc(Long caseId);
    Optional<TransferRequest> findFirstByCaseIdAndStatus(Long caseId, String status);
}
