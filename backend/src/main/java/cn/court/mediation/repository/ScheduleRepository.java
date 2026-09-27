package cn.court.mediation.repository;

import cn.court.mediation.entity.MediationSchedule;
import cn.court.mediation.enums.ScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<MediationSchedule, Long> {
    List<MediationSchedule> findByCaseIdOrderByStartTimeDesc(Long caseId);

    List<MediationSchedule> findByMediatorIdAndStatusOrderByStartTime(Long mediatorId, ScheduleStatus status);

    /** 调解员在给定时间段内的有效排期冲突检测 */
    List<MediationSchedule> findByMediatorIdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
            Long mediatorId, List<ScheduleStatus> statuses, LocalDateTime end, LocalDateTime start);
}
