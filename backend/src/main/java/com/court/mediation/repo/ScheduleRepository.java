package com.court.mediation.repo;

import com.court.mediation.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByCaseIdOrderByVersionDesc(Long caseId);

    Optional<Schedule> findFirstByCaseIdAndStatusNotOrderByVersionDesc(Long caseId, String status);
}
