package com.court.mediation.repo;

import com.court.mediation.entity.JudicialConfirm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JudicialConfirmRepository extends JpaRepository<JudicialConfirm, Long> {
    List<JudicialConfirm> findByCaseIdOrderByCreatedAtDesc(Long caseId);
}
