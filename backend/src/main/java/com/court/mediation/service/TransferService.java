package com.court.mediation.service;

import com.court.mediation.common.BizException;
import com.court.mediation.entity.*;
import com.court.mediation.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 转立案服务：一方申请或调解不成时，自动汇总从提交、分流、排期、改期、失联、缺料、
 * 调解记录、协议、送达、付款/逾期、催办、回访的全部连续过程材料，避免在调解与立案间反复补材料。
 */
@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransferRequestRepository transferRepository;
    private final CaseRepository caseRepository;
    private final TimelineRepository timelineRepository;
    private final ScheduleRepository scheduleRepository;
    private final MediationRecordRepository mediationRecordRepository;
    private final AgreementRepository agreementRepository;
    private final PaymentPlanRepository paymentPlanRepository;
    private final ServiceRecordRepository serviceRepository;
    private final VisitRecordRepository visitRepository;
    private final TimelineService timelineService;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Transactional
    public TransferRequest apply(Long caseId, String applicantName, String reasonCode, String reason) {
        CaseEntity c = caseRepository.findById(caseId).orElseThrow(() -> new BizException("案件不存在"));
        TransferRequest t = new TransferRequest();
        t.setCaseId(caseId);
        t.setApplicantName(applicantName);
        t.setReasonCode(reasonCode);
        t.setReason(reason);
        t.setStatus("SUBMITTED");
        t.setMaterialSummary(buildMaterialPackage(c));
        TransferRequest saved = transferRepository.save(t);
        timelineService.record(caseId, "TRANSFER_REQUESTED",
                "提出转立案申请，申请人：" + nz(applicantName) + "，原因：" + reasonText(reasonCode)
                        + "。系统已自动归集连续过程材料（时间线 "
                        + timelineRepository.findByCaseIdOrderByCreatedAtAscIdAsc(caseId).size()
                        + " 条），无需重复补交。", saved.getId());
        return saved;
    }

    /** 法官助理审核：接收立案，登记立案案号 */
    @Transactional
    public TransferRequest review(Long id, boolean accept, String filingNo, String opinion) {
        TransferRequest t = transferRepository.findById(id).orElseThrow(() -> new BizException("转立案申请不存在"));
        if (!"SUBMITTED".equals(t.getStatus())) throw new BizException("该申请已审核");
        if (accept) {
            t.setStatus("ACCEPTED");
            t.setFilingNo(filingNo);
            transferRepository.save(t);
            CaseEntity c = caseRepository.findById(t.getCaseId()).orElseThrow();
            c.setStatus("TRANSFERRED");
            c.setClosedAt(java.time.LocalDateTime.now());
            caseRepository.save(c);
            timelineService.record(t.getCaseId(), "TRANSFERRED",
                    "法官助理接收转立案，登记立案案号：" + nz(filingNo)
                            + "。审核意见：" + nz(opinion)
                            + "。全部诉前调解过程材料随案移送立案庭。", t.getId());
        } else {
            t.setStatus("REJECTED");
            transferRepository.save(t);
            timelineService.record(t.getCaseId(), "TRANSFERRED",
                    "转立案申请不予接收。意见：" + nz(opinion), t.getId());
        }
        return t;
    }

    /** 生成连续过程材料包（纯文本，可打印附卷） */
    @Transactional(readOnly = true)
    public String buildMaterialPackage(CaseEntity c) {
        StringBuilder sb = new StringBuilder();
        sb.append("════════ 诉前调解连续过程材料清单 ════════\n");
        sb.append("案号：").append(c.getCaseNo()).append("\n");
        sb.append("案由：").append(c.getCauseType()).append("    标题：").append(c.getTitle()).append("\n");
        sb.append("申请人：").append(nz(c.getApplicantName())).append("    被申请人：").append(nz(c.getRespondentName())).append("\n");
        sb.append("诉求金额：").append(c.getClaimAmount()).append(" 元    紧急程度：")
          .append(CaseService.urgencyText(c.getUrgency())).append("\n");
        sb.append("改期次数：").append(c.getRescheduleCount())
          .append("    被申请人失联：").append(Boolean.TRUE.equals(c.getRespondentUnreachable()) ? "是" : "否")
          .append("    材料缺失：").append(Boolean.TRUE.equals(c.getEvidenceMissing()) ? "是" : "否").append("\n");
        sb.append("当前状态：").append(CaseService.statusText(c.getStatus())).append("\n\n");

        sb.append("【一、排期与改期记录】\n");
        List<Schedule> schedules = scheduleRepository.findByCaseIdOrderByVersionDesc(c.getId());
        if (schedules.isEmpty()) sb.append("（无）\n");
        for (Schedule s : schedules) {
            sb.append("· 第").append(s.getVersion()).append("版 ")
              .append(s.getScheduledStart().format(DT))
              .append(" ").append("ONLINE".equals(s.getMode()) ? "线上" : "线下")
              .append(" 状态：").append(s.getStatus())
              .append(" 地点：").append(nz(s.getLocation())).append("\n");
        }

        sb.append("\n【二、调解笔录（争议焦点/让步/担保/履行期限）】\n");
        List<MediationRecord> records = mediationRecordRepository.findByCaseIdOrderBySessionTimeAsc(c.getId());
        if (records.isEmpty()) sb.append("（无）\n");
        for (MediationRecord r : records) {
            sb.append("· ").append(r.getSessionTime() == null ? "" : r.getSessionTime().format(DT)).append("\n");
            sb.append("  争议焦点：").append(nz(r.getFocusIssues())).append("\n");
            sb.append("  双方让步：").append(nz(r.getConcessions())).append("\n");
            if (r.getGuarantor() != null && !r.getGuarantor().isBlank())
                sb.append("  担保人：").append(r.getGuarantor()).append("\n");
            if (r.getPerformanceDeadline() != null)
                sb.append("  履行期限：").append(r.getPerformanceDeadline().format(D)).append("\n");
        }

        sb.append("\n【三、协议与签署】\n");
        List<Agreement> agreements = agreementRepository.findByCaseIdOrderByCreatedAtDesc(c.getId());
        if (agreements.isEmpty()) sb.append("（无）\n");
        for (Agreement a : agreements) {
            sb.append("· ").append(a.getAgreementNo()).append(" 金额：").append(a.getTotalAmount())
              .append(" 状态：").append(a.getStatus())
              .append(" 司法确认意向：").append(Boolean.TRUE.equals(a.getApplyJudicialConfirm()) ? "是" : "否").append("\n");
        }

        sb.append("\n【四、履行节点与逾期情况】\n");
        List<PaymentPlan> plans = paymentPlanRepository.findByCaseIdOrderByPeriodNoAsc(c.getId());
        if (plans.isEmpty()) sb.append("（无）\n");
        for (PaymentPlan p : plans) {
            sb.append("· 第").append(p.getPeriodNo()).append("期 应付日").append(p.getDueDate().format(D))
              .append(" 应付").append(p.getAmount()).append(" 已付").append(p.getPaidAmount())
              .append(" 状态：").append(planStatus(p.getStatus())).append("\n");
        }

        sb.append("\n【五、电子送达记录】\n");
        List<ServiceRecord> services = serviceRepository.findByCaseIdOrderByCreatedAtDesc(c.getId());
        if (services.isEmpty()) sb.append("（无）\n");
        for (ServiceRecord s : services) {
            sb.append("· ").append(s.getCreatedAt() == null ? "" : s.getCreatedAt().format(DT))
              .append(" 《").append(s.getDocumentName()).append("》→ ").append(nz(s.getReceiverName()))
              .append("（").append(ServiceService.channelText(s.getChannel())).append("）")
              .append(s.getStatus()).append("\n");
        }

        sb.append("\n【六、回访记录】\n");
        List<VisitRecord> visits = visitRepository.findByCaseIdOrderByVisitTimeDesc(c.getId());
        if (visits.isEmpty()) sb.append("（无）\n");
        for (VisitRecord v : visits) {
            sb.append("· ").append(v.getVisitTime() == null ? "" : v.getVisitTime().format(DT))
              .append(" ").append(VisitService.resultText(v.getResult()))
              .append("：").append(nz(v.getContent())).append("\n");
        }

        sb.append("\n【七、完整过程时间线（").append(timelineRepository.findByCaseIdOrderByCreatedAtAscIdAsc(c.getId()).size()).append("条）】\n");
        for (TimelineEvent e : timelineRepository.findByCaseIdOrderByCreatedAtAscIdAsc(c.getId())) {
            sb.append("· ").append(e.getCreatedAt().format(DT)).append(" [")
              .append(nz(e.getOperatorName())).append("/").append(roleText(e.getOperatorRole()))
              .append("] ").append(e.getContent()).append("\n");
        }
        sb.append("\n（本材料清单由诉前调解平台自动归集生成，与调解卷宗具有连续性，供法官审查。）\n");
        return sb.toString();
    }

    public static String reasonText(String code) {
        return switch (code == null ? "" : code) {
            case "UNREACHABLE" -> "被申请人失联";
            case "EVIDENCE_MISSING" -> "证据材料缺失";
            case "RESCHEDULED_MANY" -> "双方多次改期";
            case "OVERDUE" -> "付款逾期未履行";
            case "PARTIAL" -> "部分履行后拒不履行";
            case "NO_AGREEMENT" -> "调解不成";
            case "PARTY_REQUEST" -> "一方当事人申请";
            default -> code;
        };
    }

    private static String planStatus(String s) {
        return switch (s == null ? "" : s) {
            case "PAID" -> "已履行";
            case "OVERDUE" -> "已逾期";
            case "PARTIAL" -> "部分履行";
            default -> "待履行";
        };
    }

    private static String roleText(String r) {
        return switch (r == null ? "" : r) {
            case "WINDOW" -> "窗口";
            case "MEDIATOR" -> "调解员";
            case "PARTY" -> "当事人";
            case "JUDGE_ASSISTANT" -> "法官助理";
            case "EXECUTION" -> "执行联络";
            case "ADMIN" -> "管理员";
            default -> r;
        };
    }

    private static String nz(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }
}
