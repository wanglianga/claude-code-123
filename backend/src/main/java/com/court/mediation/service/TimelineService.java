package com.court.mediation.service;

import com.court.mediation.entity.TimelineEvent;
import com.court.mediation.entity.User;
import com.court.mediation.repo.TimelineRepository;
import com.court.mediation.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 时间线服务：任何角色对案件的关键动作统一记录，保证过程材料连续可追溯。
 */
@Service
@RequiredArgsConstructor
public class TimelineService {

    private final TimelineRepository timelineRepository;

    public TimelineEvent record(Long caseId, String type, String content, Long refId) {
        User u = CurrentUser.get();
        TimelineEvent e = new TimelineEvent();
        e.setCaseId(caseId);
        e.setType(type);
        e.setContent(content);
        e.setRefId(refId);
        if (u != null) {
            e.setOperatorId(u.getId());
            e.setOperatorName(u.getRealName());
            e.setOperatorRole(u.getRole());
        }
        return timelineRepository.save(e);
    }

    public TimelineEvent record(Long caseId, String type, String content) {
        return record(caseId, type, content, null);
    }
}
