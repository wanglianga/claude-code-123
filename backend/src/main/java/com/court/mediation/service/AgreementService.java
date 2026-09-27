package com.court.mediation.service;

import com.court.mediation.common.BizException;
import com.court.mediation.entity.*;
import com.court.mediation.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 协议服务：生成调解协议与分期履行计划，组织线上签署与人脸核验；
 * 双方（申请人、被申请人）均完成人脸核验签署后协议生效，案件进入履行阶段。
 */
@Service
@RequiredArgsConstructor
public class AgreementService {

    private final AgreementRepository agreementRepository;
    private final PaymentPlanRepository paymentPlanRepository;
    private final SignatureRepository signatureRepository;
    private final CaseRepository caseRepository;
    private final TimelineService timelineService;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy年M月d日");
    private static final DateTimeFormatter NO = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 起草协议（可按模板自动生成正文），同时保存付款/履行节点 */
    @Transactional
    public Agreement draft(Agreement input, List<PaymentPlan> plans) {
        CaseEntity c = caseRepository.findById(input.getCaseId()).orElseThrow(() -> new BizException("案件不存在"));
        if (input.getTotalAmount() == null) input.setTotalAmount(c.getClaimAmount());
        if (input.getContent() == null || input.getContent().isBlank()) {
            input.setContent(buildTemplate(c, input));
        }
        input.setId(null);
        input.setStatus("DRAFT");
        input.setAgreementNo("协字" + LocalDate.now().format(NO) + String.format("%04d", c.getId()));
        if (input.getMediatorId() == null) input.setMediatorId(c.getMediatorId());
        Agreement saved = agreementRepository.save(input);

        // 保存履行节点（分期）
        if (plans != null && !plans.isEmpty()) {
            int i = 1;
            for (PaymentPlan p : plans) {
                p.setId(null);
                p.setCaseId(c.getId());
                p.setAgreementId(saved.getId());
                p.setPeriodNo(i++);
                p.setStatus("PENDING");
                p.setPaidAmount(BigDecimal.ZERO);
                paymentPlanRepository.save(p);
            }
        }
        timelineService.record(c.getId(), "AGREEMENT_DRAFTED",
                "生成调解协议（草稿）" + saved.getAgreementNo() + "，协议金额 "
                        + saved.getTotalAmount() + " 元，履行方式："
                        + ("INSTALLMENTS".equals(saved.getPaymentMethod()) ? "分期履行" : "一次性履行")
                        + "，履行期限至 " + saved.getDeadline()
                        + (Boolean.TRUE.equals(saved.getApplyJudicialConfirm()) ? "，双方拟申请司法确认" : ""),
                saved.getId());
        return saved;
    }

    /**
     * 线上签署（模拟人脸核验）：facePass 为前端核验结果；服务端同时给出模拟置信度。
     * 申请人、被申请人人脸核验通过后完成签署；调解员可会签。
     */
    @Transactional
    public Signature sign(Long agreementId, String signerRole, String signerName,
                          Boolean facePass, Double faceScore, String signData, String clientIp) {
        Agreement a = agreementRepository.findById(agreementId).orElseThrow(() -> new BizException("协议不存在"));
        if ("SIGNED".equals(a.getStatus())) {
            throw new BizException("协议已完成签署，不能重复签署");
        }
        CaseEntity c = caseRepository.findById(a.getCaseId()).orElseThrow(() -> new BizException("案件不存在"));

        boolean needFace = !"MEDIATOR".equals(signerRole);
        // 模拟人脸核验：未传结果时默认通过并给出 0.96 置信度；传入 false 则拒绝签署
        boolean verified = !needFace || Boolean.TRUE.equals(facePass);
        double score = faceScore != null ? faceScore : (needFace ? 0.93 + Math.random() * 0.06 : 1.0);
        if (score < 0.90) verified = false;
        if (!verified) {
            timelineService.record(a.getCaseId(), "FACE_VERIFIED",
                    "当事人 " + nz(signerName) + " 人脸核验未通过（相似度 "
                            + String.format("%.2f", score) + "），签署被拦截。");
            throw new BizException("人脸核验未通过（相似度低于0.90），请重新核验后签署");
        }

        Signature s = new Signature();
        s.setAgreementId(agreementId);
        s.setCaseId(a.getCaseId());
        s.setSignerRole(signerRole);
        s.setSignerName(signerName);
        s.setFaceVerified(verified);
        s.setFaceScore(score);
        s.setSignData(signData != null ? signData : ("电子签名-" + signerName));
        s.setClientIp(clientIp);
        Signature saved = signatureRepository.save(s);

        timelineService.record(a.getCaseId(), "FACE_VERIFIED",
                nz(signerName) + " 完成人脸核验（相似度 " + String.format("%.2f", score) + "，通过）。");
        timelineService.record(a.getCaseId(), "SIGNED",
                nz(signerName) + "（" + roleText(signerRole) + "）线上签署调解协议 "
                        + a.getAgreementNo() + "。", agreementId);

        // 申请人与被申请人均签署后协议生效
        List<Signature> all = signatureRepository.findByAgreementIdOrderBySignedAtAsc(agreementId);
        boolean applicantSigned = all.stream().anyMatch(x -> "APPLICANT".equals(x.getSignerRole()));
        boolean respondentSigned = all.stream().anyMatch(x -> "RESPONDENT".equals(x.getSignerRole()));
        if (applicantSigned && respondentSigned && "DRAFT".equals(a.getStatus())) {
            a.setStatus("SIGNED");
            a.setSignedAt(java.time.LocalDateTime.now());
            agreementRepository.save(a);
            c.setStatus("PERFORMING");
            caseRepository.save(c);
            timelineService.record(a.getCaseId(), "SIGNED",
                    "双方签署完成，调解协议 " + a.getAgreementNo() + " 依法生效，案件进入协议履行阶段。",
                    agreementId);
        }
        return saved;
    }

    private String buildTemplate(CaseEntity c, Agreement a) {
        StringBuilder sb = new StringBuilder();
        sb.append("人民法院诉前调解协议书\n");
        sb.append("案号：").append(c.getCaseNo()).append("\n\n");
        sb.append("申请人：").append(nz(c.getApplicantName())).append("\n");
        sb.append("被申请人：").append(nz(c.getRespondentName())).append("\n\n");
        sb.append("案由：").append(c.getCauseType()).append("纠纷。\n");
        sb.append("经调解员主持调解，双方本着平等自愿原则，就如下事项达成一致：\n");
        sb.append("一、被申请人应向申请人支付款项合计人民币 ")
          .append(a.getTotalAmount() == null ? c.getClaimAmount() : a.getTotalAmount())
          .append(" 元；\n");
        sb.append("二、履行方式为").append("INSTALLMENTS".equals(a.getPaymentMethod()) ? "分期履行" : "一次性履行");
        if (a.getDeadline() != null) sb.append("，全部款项于 ").append(a.getDeadline().format(FMT)).append(" 前履行完毕");
        sb.append("；\n");
        if (a.getGuarantor() != null && !a.getGuarantor().isBlank())
            sb.append("三、担保人：").append(a.getGuarantor()).append(" 对上述债务承担担保责任；\n");
        sb.append("四、双方应按约履行，逾期申请人可凭本协议申请司法确认或强制执行；\n");
        if (Boolean.TRUE.equals(a.getApplyJudicialConfirm()))
            sb.append("五、双方一致同意自协议生效之日起三十日内向人民法院申请司法确认。\n");
        sb.append("\n申请人（签名/电子签章）：＿＿＿＿＿\n被申请人（签名/电子签章）：＿＿＿＿＿\n调解员：＿＿＿＿＿\n");
        sb.append("日期：").append(LocalDate.now().format(FMT)).append("\n");
        return sb.toString();
    }

    public static String roleText(String r) {
        return switch (r == null ? "" : r) {
            case "APPLICANT" -> "申请人";
            case "RESPONDENT" -> "被申请人";
            case "MEDIATOR" -> "调解员";
            default -> r;
        };
    }

    private static String nz(String s) {
        return s == null || s.isBlank() ? "相关方" : s;
    }
}
