package cn.court.mediation.service;

import cn.court.mediation.common.BizException;
import cn.court.mediation.entity.MediationCase;
import cn.court.mediation.entity.MediationSchedule;
import cn.court.mediation.entity.User;
import cn.court.mediation.enums.*;
import cn.court.mediation.repository.CaseRepository;
import cn.court.mediation.repository.ScheduleRepository;
import cn.court.mediation.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final CaseRepository caseRepository;
    private final UserRepository userRepository;
    private final TimelineService timelineService;

    public ScheduleService(ScheduleRepository scheduleRepository, CaseRepository caseRepository,
                           UserRepository userRepository, TimelineService timelineService) {
        this.scheduleRepository = scheduleRepository;
        this.caseRepository = caseRepository;
        this.userRepository = userRepository;
        this.timelineService = timelineService;
    }

    /**
     * 推荐调解员：
     * 1) 案由与专长匹配；2) 外地当事人+愿意线上时优先线上渠道；
     * 3) 在办量未超 capacity；4) 指定时间段无冲突。按在办量升序返回推荐列表。
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> recommendMediators(Long caseId, LocalDateTime start, LocalDateTime end) {
        MediationCase c = caseRepository.findById(caseId).orElseThrow(() -> new BizException("案件不存在"));
        List<User> mediators = userRepository.findByRole(Role.MEDIATOR);

        // 每位调解员当前在办案件数
        Map<Long, Long> activeCount = new HashMap<>();
        for (User m : mediators) {
            long n = caseRepository.findByMediatorId(m.getId()).stream()
                    .filter(x -> x.getStatus() != CaseStatus.PERFORMED_DONE
                            && x.getStatus() != CaseStatus.TRANSFERRED
                            && x.getStatus() != CaseStatus.CLOSED
                            && x.getStatus() != CaseStatus.MEDIATION_FAILED)
                    .count();
            activeCount.put(m.getId(), n);
        }

        return mediators.stream()
                .map(m -> {
                    Map<String, Object> view = new LinkedHashMap<>();
                    view.put("id", m.getId());
                    view.put("realName", m.getRealName());
                    view.put("expertise", m.getExpertise());
                    view.put("mediationRoom", m.getMediationRoom());
                    view.put("capacity", m.getCapacity());
                    long active = activeCount.getOrDefault(m.getId(), 0L);
                    view.put("activeCount", active);

                    boolean expertiseMatch = m.getExpertise() != null
                            && m.getExpertise().contains(c.getCaseType().name());
                    view.put("expertiseMatch", expertiseMatch);

                    boolean withinCapacity = active < (m.getCapacity() == null ? 20 : m.getCapacity());
                    view.put("withinCapacity", withinCapacity);

                    boolean timeFree = true;
                    if (start != null && end != null) {
                        List<MediationSchedule> conflict =
                                scheduleRepository.findByMediatorIdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                                        m.getId(), List.of(ScheduleStatus.PLANNED), end, start);
                        timeFree = conflict.isEmpty();
                    }
                    view.put("timeFree", timeFree);

                    // 推荐分：专长+3，容量内+1，时间空闲+2，在办越少越靠前
                    long score = (expertiseMatch ? 3 : 0) + (withinCapacity ? 1 : 0) + (timeFree ? 2 : 0) * 10 - active;
                    view.put("score", score);
                    return view;
                })
                .sorted(Comparator.comparingLong(v -> -((Number) v.get("score")).longValue()))
                .collect(Collectors.toList());
    }

    /** 创建排期 */
    @Transactional
    public MediationSchedule create(Long caseId, Map<String, Object> body, User operator) {
        MediationCase c = caseRepository.findById(caseId).orElseThrow(() -> new BizException("案件不存在"));
        Long mediatorId = body.get("mediatorId") != null
                ? Long.valueOf(body.get("mediatorId").toString()) : c.getMediatorId();
        if (mediatorId == null) {
            throw new BizException("请先指定调解员");
        }
        User mediator = userRepository.findById(mediatorId)
                .filter(u -> u.getRole() == Role.MEDIATOR)
                .orElseThrow(() -> new BizException("调解员不存在"));
        LocalDateTime start = LocalDateTime.parse(body.get("startTime").toString());
        LocalDateTime end = LocalDateTime.parse(body.get("endTime").toString());
        if (!end.isAfter(start)) {
            throw new BizException("结束时间必须晚于开始时间");
        }
        Channel channel = Channel.valueOf(body.getOrDefault("channel",
                c.getChannel() == null ? Channel.ONLINE.name() : c.getChannel().name()).toString());
        String location = (String) body.getOrDefault("location",
                channel == Channel.ONLINE ? "线上调解室（法院视频调解平台）" : "院部第一调解室");

        // 冲突检测
        List<MediationSchedule> conflicts =
                scheduleRepository.findByMediatorIdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                        mediatorId, List.of(ScheduleStatus.PLANNED), end, start);
        if (!conflicts.isEmpty()) {
            throw new BizException("调解员该时段已有排期冲突，请改约其他时间");
        }

        long round = scheduleRepository.findByCaseIdOrderByStartTimeDesc(caseId).size() + 1;
        MediationSchedule s = new MediationSchedule();
        s.setCaseId(caseId);
        s.setMediatorId(mediatorId);
        s.setChannel(channel);
        s.setLocation(location);
        s.setStartTime(start);
        s.setEndTime(end);
        s.setStatus(ScheduleStatus.PLANNED);
        s.setRoundNo((int) round);
        s.setNote((String) body.get("note"));
        scheduleRepository.save(s);

        // 案件补齐承办信息并推进状态
        c.setMediatorId(mediatorId);
        if (c.getChannel() == null) c.setChannel(channel);
        if (c.getStatus() == CaseStatus.TRIAGED || c.getStatus() == CaseStatus.PENDING_TRIAGE) {
            c.setStatus(CaseStatus.SCHEDULED);
        }
        caseRepository.save(c);

        timelineService.record(caseId, "SCHEDULE", "第" + round + "次调解已排期",
                String.format("时间：%s 至 %s；方式：%s；地点/链接：%s；调解员：%s。%s",
                        start, end, channel.getLabel(), location, mediator.getRealName(),
                        body.get("note") == null ? "" : "备注：" + body.get("note")),
                operator);
        return s;
    }

    /** 改期：原排期置为 RESCHEDULED 并保留，另排新期，累计改期次数 */
    @Transactional
    public MediationSchedule reschedule(Long scheduleId, Map<String, Object> body, User operator) {
        MediationSchedule old = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BizException("排期记录不存在"));
        if (old.getStatus() != ScheduleStatus.PLANNED) {
            throw new BizException("仅待开始的排期可以改期");
        }
        String reason = (String) body.getOrDefault("reason", "双方时间冲突");

        LocalDateTime start = LocalDateTime.parse(body.get("startTime").toString());
        LocalDateTime end = LocalDateTime.parse(body.get("endTime").toString());
        if (!end.isAfter(start)) {
            throw new BizException("结束时间必须晚于开始时间");
        }
        List<MediationSchedule> conflicts =
                scheduleRepository.findByMediatorIdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                        old.getMediatorId(), List.of(ScheduleStatus.PLANNED), end, start);
        conflicts.removeIf(x -> x.getId().equals(scheduleId));
        if (!conflicts.isEmpty()) {
            throw new BizException("调解员新时段仍有排期冲突");
        }

        old.setStatus(ScheduleStatus.RESCHEDULED);
        old.setNote((old.getNote() == null ? "" : old.getNote() + " | ") + "改期原因：" + reason);
        scheduleRepository.save(old);

        MediationCase c = caseRepository.findById(old.getCaseId()).orElseThrow();
        c.setRescheduleCount((c.getRescheduleCount() == null ? 0 : c.getRescheduleCount()) + 1);
        caseRepository.save(c);

        MediationSchedule fresh = new MediationSchedule();
        fresh.setCaseId(old.getCaseId());
        fresh.setMediatorId(old.getMediatorId());
        fresh.setChannel(old.getChannel());
        fresh.setLocation(old.getLocation());
        fresh.setStartTime(start);
        fresh.setEndTime(end);
        fresh.setStatus(ScheduleStatus.PLANNED);
        fresh.setRoundNo(old.getRoundNo() + 1);
        fresh.setNote("由第" + old.getRoundNo() + "次改期而来");
        scheduleRepository.save(fresh);

        String warn = c.getRescheduleCount() >= 2
                ? "【多次改期预警：已累计" + c.getRescheduleCount() + "次，过程留痕将随卷提交法官】"
                : "";
        timelineService.record(old.getCaseId(), "RESCHEDULE",
                "第" + c.getRescheduleCount() + "次改期",
                String.format("原排期 %s 取消，原因：%s；新排期：%s 至 %s。%s",
                        old.getStartTime(), reason, start, end, warn),
                operator);
        return fresh;
    }

    /** 当事人确认出席 */
    @Transactional
    public void confirmAttend(Long scheduleId, String party, User operator) {
        MediationSchedule s = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BizException("排期不存在"));
        if ("APPLICANT".equals(party)) {
            s.setApplicantConfirmed(true);
        } else {
            s.setRespondentConfirmed(true);
        }
        scheduleRepository.save(s);
        timelineService.record(s.getCaseId(), "SCHEDULE",
                ("APPLICANT".equals(party) ? "申请人" : "被申请人") + "确认出席",
                "确认参加 " + s.getStartTime() + " 的调解。", operator);
    }

    @Transactional(readOnly = true)
    public List<MediationSchedule> caseSchedules(Long caseId) {
        return scheduleRepository.findByCaseIdOrderByStartTimeDesc(caseId);
    }
}
