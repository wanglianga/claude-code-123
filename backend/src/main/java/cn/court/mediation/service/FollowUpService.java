package cn.court.mediation.service;

import cn.court.mediation.common.BizException;
import cn.court.mediation.entity.FollowUp;
import cn.court.mediation.entity.MediationCase;
import cn.court.mediation.enums.CaseStatus;
import cn.court.mediation.repository.CaseRepository;
import cn.court.mediation.repository.FollowUpRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

/** 履行回访：执行联络/法官助理跟踪分期与逾期案件 */
@Service
public class FollowUpService {

    private final FollowUpRepository followUpRepository;
    private final CaseRepository caseRepository;
    private final TimelineService timelineService;

    public FollowUpService(FollowUpRepository followUpRepository, CaseRepository caseRepository,
                           TimelineService timelineService) {
        this.followUpRepository = followUpRepository;
        this.caseRepository = caseRepository;
        this.timelineService = timelineService;
    }

    @Transactional
    public FollowUp create(Long caseId, Map<String, Object> body, cn.court.mediation.entity.User operator) {
        MediationCase c = caseRepository.findById(caseId).orElseThrow(() -> new BizException("案件不存在"));
        FollowUp f = new FollowUp();
        f.setCaseId(caseId);
        f.setOperatorId(operator.getId());
        f.setOperatorName(operator.getRealName());
        f.setFollowUpDate(body.get("followUpDate") != null ? LocalDate.parse(body.get("followUpDate").toString()) : LocalDate.now());
        f.setResult((String) body.getOrDefault("result", "NORMAL"));
        f.setWay((String) body.getOrDefault("way", "PHONE"));
        f.setNote((String) body.getOrDefault("note", ""));
        followUpRepository.save(f);

        c.setLastFollowUpDate(f.getFollowUpDate());
        c.setLastFollowUpResult(f.getNote());
        // 回访确认履行完毕可推动状态
        if ("DONE".equals(f.getResult()) && c.getStatus() == CaseStatus.OVERDUE) {
            c.setStatus(CaseStatus.PERFORMING);
        }
        caseRepository.save(c);

        String resultText = switch (f.getResult()) {
            case "DONE" -> "已履行完毕";
            case "PARTIAL" -> "部分履行";
            case "OVERDUE" -> "仍处逾期";
            case "OBJECTION" -> "当事人提出异议";
            default -> "履约正常";
        };
        timelineService.record(caseId, "FOLLOWUP", "履行回访（" + resultText + "）",
                "回访日期：" + f.getFollowUpDate() + "；方式：" + f.getWay() + "；情况：" + f.getNote(), operator);
        return f;
    }
}
