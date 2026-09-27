package cn.court.mediation.repository;

import cn.court.mediation.entity.FollowUp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FollowUpRepository extends JpaRepository<FollowUp, Long> {
    List<FollowUp> findByCaseIdOrderByFollowUpDateDesc(Long caseId);
}
