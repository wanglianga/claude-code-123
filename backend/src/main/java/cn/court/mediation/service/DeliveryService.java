package cn.court.mediation.service;

import cn.court.mediation.common.BizException;
import cn.court.mediation.entity.DeliveryRecord;
import cn.court.mediation.entity.MediationCase;
import cn.court.mediation.repository.CaseRepository;
import cn.court.mediation.repository.DeliveryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/** 电子送达：排期通知、协议、司法确认文书等 */
@Service
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final CaseRepository caseRepository;
    private final TimelineService timelineService;

    public DeliveryService(DeliveryRepository deliveryRepository, CaseRepository caseRepository,
                           TimelineService timelineService) {
        this.deliveryRepository = deliveryRepository;
        this.caseRepository = caseRepository;
        this.timelineService = timelineService;
    }

    @Transactional
    public DeliveryRecord send(Long caseId, Map<String, Object> body, cn.court.mediation.entity.User operator) {
        MediationCase c = caseRepository.findById(caseId).orElseThrow(() -> new BizException("案件不存在"));
        String target = (String) body.getOrDefault("target", "RESPONDENT");
        DeliveryRecord d = new DeliveryRecord();
        d.setCaseId(caseId);
        d.setTarget(target);
        if ("APPLICANT".equals(target)) {
            d.setTargetName(c.getApplicantName());
            d.setTargetPhone(c.getApplicantPhone());
        } else {
            d.setTargetName(c.getRespondentName());
            d.setTargetPhone(c.getRespondentPhone());
        }
        d.setDocumentName((String) body.getOrDefault("documentName", "调解事项通知"));
        d.setChannel((String) body.getOrDefault("channel", "SMS"));
        // 演示：手机号为空（如失联情形）时送达失败，其余成功
        if (d.getTargetPhone() == null || d.getTargetPhone().isBlank()) {
            d.setStatus("FAILED");
            d.setDetail("无有效联系方式，电子送达失败，已转为其他送达方式提示");
        } else {
            d.setStatus("SENT");
            d.setDetail("已通过" + ("SMS".equals(d.getChannel()) ? "短信" : "EMAIL".equals(d.getChannel()) ? "邮件" : "站内平台")
                    + "发送至" + d.getTargetPhone() + "，系统将记录签收状态");
            d.setReadAt(LocalDateTime.now().plusMinutes(3));
        }
        d.setSentAt(LocalDateTime.now());
        deliveryRepository.save(d);

        timelineService.record(caseId, "DELIVERY",
                "电子送达：" + d.getDocumentName() + "（" + ("APPLICANT".equals(target) ? "申请人" : "被申请人") + "）",
                d.getDetail() + "。", operator);
        return d;
    }
}
