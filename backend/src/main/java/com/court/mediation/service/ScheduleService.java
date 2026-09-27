package com.court.mediation.service;

import com.court.mediation.common.BizException;
import com.court.mediation.entity.CaseEntity;
import com.court.mediation.entity.Schedule;
import com.court.mediation.entity.User;
import com.court.mediation.repo.CaseRepository;
import com.court.mediation.repo.ScheduleRepository;
import com.court.mediation.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 调解排期：综合案由、调解员专长、双方可用时间、是否外地当事人，安排线上/线下调解。
 * 改期采用版本化记录，旧排期置 CANCELLED 并累计改期次数。
 */
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final CaseRepository caseRepository;
    private final UserRepository userRepository;
    private final TimelineService timelineService;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 推荐与案由匹配的在册调解员（前端分流/排期时选用） */
    public List<Map<String, Object>> recommendMediators(String causeType) {
        List<User> mediators = userRepository.findByRoleAndEnabledTrue("MEDIATOR");
        List<Map<String, Object>> result = new ArrayList<>();
        for (User m : mediators) {
            boolean match = causeType != null && m.getSpecialties() != null
                    && m.getSpecialties().contains(causeType);
            result.add(Map.of(
                    "id", m.getId(),
                    "realName", m.getRealName() == null ? m.getUsername() : m.getRealName(),
                    "specialties", m.getSpecialties() == null ? "" : m.getSpecialties(),
                    "title", m.getTitle() == null ? "" : m.getTitle(),
                    "remote", Boolean.TRUE.equals(m.getRemote()),
                    "matched", match
            ));
        }
        result.sort(Comparator.comparing((Map<String, Object> x) -> !(Boolean) x.get("matched")));
        return result;
    }

    @Transactional
    public Schedule create(Long caseId, LocalDateTime start, LocalDateTime end,
                           String mode, String location, String note, boolean confirmed) {
        CaseEntity c = mustGet(caseId);
        if (c.getMediatorId() == null) {
            throw new BizException("案件尚未分流指派调解员，无法排期");
        }
        if (start == null) throw new BizException("调解开始时间不能为空");
        if (end == null) end = start.plusHours(2);

        // 若已有有效排期，则本次视为"改期"：旧记录取消并版本递增
        List<Schedule> all = scheduleRepository.findByCaseIdOrderByVersionDesc(caseId);
        Schedule latest = all.stream().filter(s -> !"CANCELLED".equals(s.getStatus())).findFirst().orElse(null);

        Schedule s = new Schedule();
        s.setCaseId(caseId);
        s.setMediatorId(c.getMediatorId());
        s.setMode(mode != null ? mode : (c.getMediationMode() == null ? "ONLINE" : c.getMediationMode()));
        s.setScheduledStart(start);
        s.setScheduledEnd(end);
        s.setLocation(location);
        s.setNote(note);
        s.setPartyConfirmed(confirmed);
        s.setStatus(confirmed ? "CONFIRMED" : "PROPOSED");
        s.setCreatedBy(com.court.mediation.security.CurrentUser.userId());
        s.setVersion(latest == null ? 1 : latest.getVersion() + 1);
        Schedule saved = scheduleRepository.save(s);

        String timeText = start.format(DT) + " 至 " + end.format(DT)
                + ("ONLINE".equals(s.getMode()) ? "（线上）" : "（线下）");
        if (latest != null) {
            latest.setStatus("CANCELLED");
            scheduleRepository.save(latest);
            c.setRescheduleCount(c.getRescheduleCount() == null ? 1 : c.getRescheduleCount() + 1);
            caseRepository.save(c);
            timelineService.record(caseId, "RESCHEDULED",
                    "第 " + c.getRescheduleCount() + " 次改期，新调解时间：" + timeText
                            + "，地点/链接：" + nz(location)
                            + "。多次改期将作为连续过程材料随案留档。", saved.getId());
        } else {
            timelineService.record(caseId, "SCHEDULED",
                    "完成首次排期：" + timeText + "，地点/链接：" + nz(location)
                            + "，状态：" + (confirmed ? "双方已确认可用时间" : "待当事人确认"), saved.getId());
        }
        if (confirmed && "PENDING_SCHEDULE".equals(c.getStatus())) {
            c.setStatus("SCHEDULED");
            caseRepository.save(c);
        }
        return saved;
    }

    @Transactional
    public Schedule confirm(Long scheduleId) {
        Schedule s = scheduleRepository.findById(scheduleId).orElseThrow(() -> new BizException("排期不存在"));
        s.setPartyConfirmed(true);
        if ("PROPOSED".equals(s.getStatus())) s.setStatus("CONFIRMED");
        scheduleRepository.save(s);
        CaseEntity c = mustGet(s.getCaseId());
        if ("PENDING_SCHEDULE".equals(c.getStatus())) {
            c.setStatus("SCHEDULED");
            caseRepository.save(c);
        }
        timelineService.record(s.getCaseId(), "SCHEDULED", "当事人确认调解时间："
                + s.getScheduledStart().format(DT) + "，排期已生效。", s.getId());
        return s;
    }

    @Transactional
    public Schedule complete(Long scheduleId, String note) {
        Schedule s = scheduleRepository.findById(scheduleId).orElseThrow(() -> new BizException("排期不存在"));
        s.setStatus("COMPLETED");
        if (note != null) s.setNote(note);
        scheduleRepository.save(s);
        CaseEntity c = mustGet(s.getCaseId());
        if (!List.of("AGREED", "PERFORMING", "COMPLETED", "TRANSFERRED", "FAILED").contains(c.getStatus())) {
            c.setStatus("MEDIATING");
            caseRepository.save(c);
        }
        timelineService.record(s.getCaseId(), "SESSION_HELD",
                "调解会议如期召开并结束。" + (note == null ? "" : "备注：" + note), s.getId());
        return s;
    }

    private CaseEntity mustGet(Long id) {
        return caseRepository.findById(id).orElseThrow(() -> new BizException("案件不存在"));
    }

    private static String nz(String s) {
        return s == null || s.isBlank() ? "待补充" : s;
    }
}
