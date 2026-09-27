package cn.court.mediation.service;

import cn.court.mediation.entity.TimelineEvent;
import cn.court.mediation.entity.User;
import cn.court.mediation.repository.TimelineRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/** 统一时间线记录：所有角色围绕同一案件的动作都写入这里，保证过程材料连续 */
@Service
public class TimelineService {

    private final TimelineRepository timelineRepository;

    public TimelineService(TimelineRepository timelineRepository) {
        this.timelineRepository = timelineRepository;
    }

    public void record(Long caseId, String type, String title, String detail, User operator, String attachment) {
        TimelineEvent e = new TimelineEvent();
        e.setCaseId(caseId);
        e.setType(type);
        e.setTitle(title);
        e.setDetail(detail);
        e.setAttachmentName(attachment);
        if (operator != null) {
            e.setOperatorId(operator.getId());
            e.setOperatorName(operator.getRealName());
            e.setOperatorRole(operator.getRole().name());
        } else {
            e.setOperatorName("系统");
            e.setOperatorRole("SYSTEM");
        }
        e.setCreatedAt(LocalDateTime.now());
        timelineRepository.save(e);
    }

    public void record(Long caseId, String type, String title, String detail, User operator) {
        record(caseId, type, title, detail, operator, null);
    }
}
