package com.court.mediation.repo;

import com.court.mediation.entity.TimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimelineRepository extends JpaRepository<TimelineEvent, Long> {
    List<TimelineEvent> findByCaseIdOrderByCreatedAtAscIdAsc(Long caseId);
}
