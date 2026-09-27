package com.court.mediation.service;

import com.court.mediation.common.BizException;
import com.court.mediation.entity.CaseEntity;
import com.court.mediation.entity.Evidence;
import com.court.mediation.entity.MediationRecord;
import com.court.mediation.repo.CaseRepository;
import com.court.mediation.repo.EvidenceRepository;
import com.court.mediation.repo.MediationRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 调解过程服务：调解员记录争议焦点、双方让步、付款计划、担保人、履行期限、司法确认意向；
 * 并登记被申请人失联、证据缺失等影响推进的异常情况，全部同步到案件时间线。
 */
@Service
@RequiredArgsConstructor
public class MediationService {

    private final MediationRecordRepository recordRepository;
    private final EvidenceRepository evidenceRepository;
    private final CaseRepository caseRepository;
    private final TimelineService timelineService;

    @Transactional
    public MediationRecord saveRecord(MediationRecord r) {
        CaseEntity c = caseRepository.findById(r.getCaseId()).orElseThrow(() -> new BizException("案件不存在"));
        if (r.getMediatorId() == null) r.setMediatorId(c.getMediatorId());
        MediationRecord saved = recordRepository.save(r);

        StringBuilder sb = new StringBuilder("调解员录入调解记录：");
        sb.append("争议焦点【").append(nz(r.getFocusIssues())).append("】；");
        sb.append("双方让步【").append(nz(r.getConcessions())).append("】；");
        if (r.getGuarantor() != null && !r.getGuarantor().isBlank())
            sb.append("担保人：").append(r.getGuarantor()).append("；");
        if (r.getPerformanceDeadline() != null)
            sb.append("履行期限至 ").append(r.getPerformanceDeadline()).append("；");
        sb.append("调解结果：").append(switch (nz(r.getResult())) {
            case "AGREED" -> "达成一致";
            case "NO_AGREEMENT" -> "未达成一致";
            default -> "继续调解";
        });
        sb.append("；申请司法确认：").append(Boolean.TRUE.equals(r.getApplyJudicialConfirm()) ? "是" : "否");
        timelineService.record(r.getCaseId(), "MEDIATION_NOTE", sb.toString(), saved.getId());

        if ("AGREED".equals(r.getResult())) {
            if (!List.of("AGREED", "PERFORMING", "COMPLETED").contains(c.getStatus())) {
                c.setStatus("AGREED");
                caseRepository.save(c);
            }
        } else if ("NO_AGREEMENT".equals(r.getResult()) && "SCHEDULED".equals(c.getStatus())) {
            c.setStatus("MEDIATING");
            caseRepository.save(c);
        }
        return saved;
    }

    /** 登记被申请人失联 */
    @Transactional
    public CaseEntity markUnreachable(Long caseId, String remark) {
        CaseEntity c = get(caseId);
        c.setRespondentUnreachable(true);
        caseRepository.save(c);
        timelineService.record(caseId, "UNREACHABLE",
                "登记被申请人失联：被申请人 " + nz(c.getRespondentName())
                        + "（电话 " + nz(c.getRespondentPhone()) + "）多次联系未果。"
                        + "处置说明：" + nz(remark)
                        + "。该记录将作为电子送达/转立案连续材料。");
        return c;
    }

    /** 恢复联系 */
    @Transactional
    public CaseEntity markReached(Long caseId, String remark) {
        CaseEntity c = get(caseId);
        c.setRespondentUnreachable(false);
        caseRepository.save(c);
        timelineService.record(caseId, "REACHED",
                "被申请人恢复联系。说明：" + nz(remark));
        return c;
    }

    /** 登记证据材料缺失 */
    @Transactional
    public CaseEntity markEvidenceMissing(Long caseId, String remark) {
        CaseEntity c = get(caseId);
        c.setEvidenceMissing(true);
        caseRepository.save(c);
        timelineService.record(caseId, "EVIDENCE_MISSING",
                "登记证据材料缺失，需当事人补正：" + nz(remark)
                        + "。补正完成前案件挂起，避免在调解与立案之间反复补材料。");
        return c;
    }

    /** 补充证据 */
    @Transactional
    public Evidence addEvidence(Evidence e) {
        get(e.getCaseId());
        if (e.getVerified() == null) e.setVerified(false);
        Evidence saved = evidenceRepository.save(e);
        boolean stillMissing = evidenceRepository.findByCaseIdOrderByCreatedAtAsc(e.getCaseId()).stream()
                .anyMatch(x -> !Boolean.TRUE.equals(x.getVerified()) && x.getName().contains("待补"));
        CaseEntity c = get(e.getCaseId());
        if (Boolean.TRUE.equals(c.getEvidenceMissing()) && !stillMissing) {
            c.setEvidenceMissing(false);
            caseRepository.save(c);
        }
        timelineService.record(e.getCaseId(), "EVIDENCE_SUPPLEMENTED",
                "补充/核验证据：" + saved.getName()
                        + "（" + nz(saved.getEvidenceType()) + "）"
                        + (Boolean.TRUE.equals(saved.getVerified()) ? "，已经质证核验" : "")
                        + "。" + nz(saved.getRemark()), saved.getId());
        return saved;
    }

    private CaseEntity get(Long id) {
        return caseRepository.findById(id).orElseThrow(() -> new BizException("案件不存在"));
    }

    private static String nz(String s) {
        return s == null || s.isBlank() ? "无" : s;
    }
}
