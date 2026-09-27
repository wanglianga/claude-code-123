package com.court.mediation.service;

import com.court.mediation.common.BizException;
import com.court.mediation.entity.CaseEntity;
import com.court.mediation.entity.ServiceRecord;
import com.court.mediation.repo.CaseRepository;
import com.court.mediation.repo.ServiceRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 电子送达：调解通知、排期通知、协议书、履行催告、转立案告知等文书的短信/站内送达 */
@Service
@RequiredArgsConstructor
public class ServiceService {

    private final ServiceRecordRepository repository;
    private final CaseRepository caseRepository;
    private final TimelineService timelineService;

    @Transactional
    public ServiceRecord serve(Long caseId, String documentName, String receiverName,
                               String receiverPhone, String channel, String content) {
        CaseEntity c = caseRepository.findById(caseId).orElseThrow(() -> new BizException("案件不存在"));
        ServiceRecord s = new ServiceRecord();
        s.setCaseId(caseId);
        s.setDocumentName(documentName);
        s.setReceiverName(receiverName != null ? receiverName : c.getRespondentName());
        s.setReceiverPhone(receiverPhone != null ? receiverPhone : c.getRespondentPhone());
        s.setChannel(channel == null ? "ONLINE" : channel);
        s.setContent(content);
        s.setOperatorId(com.court.mediation.security.CurrentUser.userId());
        // 电子送达发起即视为到达受送达人特定系统（演示直接标记送达）
        s.setStatus("DELIVERED");
        s.setDeliveredAt(LocalDateTime.now());
        ServiceRecord saved = repository.save(s);
        timelineService.record(caseId, "SERVED",
                "电子送达《" + documentName + "》→ 受送达人 " + saved.getReceiverName()
                        + "（" + channelText(saved.getChannel()) + " " + nz(saved.getReceiverPhone()) + "），状态：已送达。",
                saved.getId());
        return saved;
    }

    @Transactional
    public ServiceRecord markRead(Long id) {
        ServiceRecord s = repository.findById(id).orElseThrow(() -> new BizException("送达记录不存在"));
        s.setStatus("READ");
        s.setReadAt(LocalDateTime.now());
        return repository.save(s);
    }

    public static String channelText(String c) {
        return switch (c == null ? "" : c) {
            case "SMS" -> "短信";
            case "EMAIL" -> "邮件";
            default -> "平台站内";
        };
    }

    private static String nz(String s) {
        return s == null || s.isBlank() ? "未留存号码" : s;
    }
}
