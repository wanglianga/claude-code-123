package com.court.mediation.repo;

import com.court.mediation.entity.Evidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvidenceRepository extends JpaRepository<Evidence, Long> {
    List<Evidence> findByCaseIdOrderByCreatedAtAsc(Long caseId);
}
