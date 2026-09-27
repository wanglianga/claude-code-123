package cn.court.mediation.repository;

import cn.court.mediation.entity.TimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimelineRepository extends JpaRepository<TimelineEvent, Long> {
    List<TimelineEvent> findByCaseIdOrderByCreatedAtAsc(Long caseId);
    long countByCaseIdAndType(Long caseId, String type);
}
