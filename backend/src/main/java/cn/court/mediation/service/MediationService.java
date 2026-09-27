package cn.court.mediation.service;

import cn.court.mediation.common.BizException;
import cn.court.mediation.entity.*;
import cn.court.mediation.enums.CaseStatus;
import cn.court.mediation.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class MediationService {

    private final MediationRecordRepository recordRepository;
    private final AgreementRepository agreementRepository;
    private final CaseRepository caseRepository;
    private final ScheduleRepository scheduleRepository;
    private final FaceVerificationRepository faceRepository;
    private final TimelineService timelineService;

    public MediationService(MediationRecordRepository recordRepository, AgreementRepository agreementRepository,
                            CaseRepository caseRepository, ScheduleRepository scheduleRepository,
                            FaceVerificationRepository faceRepository, TimelineService timelineService) {
        this.recordRepository = recordRepository;
        this.agreementRepository = agreementRepository;
        this.caseRepository = caseRepository;
        this.scheduleRepository = scheduleRepository;
        this.faceRepository = faceRepository;
        this.timelineService = timelineService;
    }

    /** 保存一次调解笔录/过程记录 */
    @Transactional
    public MediationRecord saveRecord(Long caseId, Map<String, Object> body, User operator) {
        MediationCase c = caseRepository.findById(caseId).orElseThrow(() -> new BizException("案件不存在"));
        MediationRecord r = new MediationRecord();
        r.setCaseId(caseId);
        if (body.get("scheduleId") != null) {
            r.setScheduleId(Long.valueOf(body.get("scheduleId").toString()));
        }
        r.setMediatorId(operator.getId());
        List<MediationRecord> old = recordRepository.findByCaseIdOrderByRoundNoDescCreatedAtDesc(caseId);
        r.setRoundNo(old.isEmpty() ? 1 : old.get(0).getRoundNo() + 1);
        r.setFocusPoints((String) body.getOrDefault("focusPoints", ""));
        r.setApplicantConcession((String) body.getOrDefault("applicantConcession", ""));
        r.setRespondentConcession((String) body.getOrDefault("respondentConcession", ""));
        r.setProcessNote((String) body.getOrDefault("processNote", ""));
        String outcome = (String) body.getOrDefault("outcome", "CONTINUE");
        r.setOutcome(outcome);
        recordRepository.save(r);

        // 关联排期标记完成
        if (r.getScheduleId() != null) {
            scheduleRepository.findById(r.getScheduleId()).ifPresent(s -> {
                s.setStatus(cn.court.mediation.enums.ScheduleStatus.FINISHED);
                scheduleRepository.save(s);
            });
        }
        c.setStatus(CaseStatus.MEDIATING);
        caseRepository.save(c);

        String outcomeText = switch (outcome) {
            case "SUCCESS" -> "达成一致，拟签订调解协议";
            case "FAIL" -> "未能达成一致";
            default -> "调解继续，另行安排";
        };
        timelineService.record(caseId, "RECORD", "第" + r.getRoundNo() + "次调解笔录",
                String.format("争议焦点：%s；申请人让步：%s；被申请人让步：%s；结果：%s",
                        blank(r.getFocusPoints()), blank(r.getApplicantConcession()),
                        blank(r.getRespondentConcession()), outcomeText),
                operator);
        return r;
    }

    private String blank(String s) {
        return s == null || s.isBlank() ? "（未记录）" : s;
    }

    /** 起草/更新调解协议（含付款计划、担保人、履行期限、司法确认意向） */
    @Transactional
    public MediationAgreement draftAgreement(Long caseId, Map<String, Object> body, User operator) {
        MediationCase c = caseRepository.findById(caseId).orElseThrow(() -> new BizException("案件不存在"));
        MediationAgreement a = agreementRepository.findByCaseId(caseId).orElseGet(MediationAgreement::new);
        a.setCaseId(caseId);
        a.setTotalAmount(new java.math.BigDecimal(body.get("totalAmount").toString()));
        a.setInstallment(Boolean.TRUE.equals(body.get("installment")));
        a.setPaymentPlan((String) body.getOrDefault("paymentPlan", ""));
        if (body.get("deadline") != null) {
            a.setDeadline(java.time.LocalDate.parse(body.get("deadline").toString()));
        }
        a.setGuarantorName((String) body.getOrDefault("guarantorName", ""));
        a.setGuarantorPhone((String) body.getOrDefault("guarantorPhone", ""));
        a.setGuarantorNote((String) body.getOrDefault("guarantorNote", ""));
        a.setJudicialConfirmRequested(Boolean.TRUE.equals(body.get("judicialConfirmRequested")));
        a.setContent(buildAgreementText(c, a, body));
        agreementRepository.save(a);

        c.setStatus(CaseStatus.MEDIATION_SUCCESS);
        caseRepository.save(c);

        timelineService.record(caseId, "AGREEMENT", "调解协议起草完成",
                String.format("协议金额%s元，%s，履行期限至%s。担保人：%s。%s",
                        a.getTotalAmount().toPlainString(),
                        Boolean.TRUE.equals(a.getInstallment()) ? "分期履行" : "一次性履行",
                        a.getDeadline() == null ? "未约定" : a.getDeadline(),
                        a.getGuarantorName() == null || a.getGuarantorName().isBlank() ? "无" : a.getGuarantorName(),
                        Boolean.TRUE.equals(a.getJudicialConfirmRequested()) ? "当事人申请司法确认。" : ""),
                operator);
        return a;
    }

    private String buildAgreementText(MediationCase c, MediationAgreement a, Map<String, Object> body) {
        String customContent = (String) body.get("content");
        if (customContent != null && !customContent.isBlank()) {
            return customContent;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("人民法院诉前调解协议书\n\n");
        sb.append("案号：").append(c.getCaseNo()).append("\n");
        sb.append("案由：").append(c.getCaseType().getLabel()).append("\n");
        sb.append("申请人：").append(c.getApplicantName())
                .append(c.getApplicantPhone() == null ? "" : "（" + c.getApplicantPhone() + "）").append("\n");
        sb.append("被申请人：").append(c.getRespondentName())
                .append(c.getRespondentPhone() == null ? "" : "（" + c.getRespondentPhone() + "）").append("\n\n");
        sb.append("经调解组织主持调解，双方自愿达成如下协议：\n");
        sb.append("一、被申请人").append(c.getRespondentName()).append("应向申请人")
                .append(c.getApplicantName()).append("支付人民币")
                .append(a.getTotalAmount()).append("元。\n");
        sb.append("二、履行方式：").append(Boolean.TRUE.equals(a.getInstallment()) ? "分期履行" : "一次性履行")
                .append("。付款计划：").append(a.getPaymentPlan() == null ? "" : a.getPaymentPlan()).append("\n");
        if (a.getDeadline() != null) {
            sb.append("三、全部款项履行期限至 ").append(a.getDeadline()).append("。\n");
        }
        if (a.getGuarantorName() != null && !a.getGuarantorName().isBlank()) {
            sb.append("四、担保人：").append(a.getGuarantorName());
            if (a.getGuarantorPhone() != null) sb.append("，联系电话：").append(a.getGuarantorPhone());
            sb.append("，对上述债务承担担保责任。\n");
        }
        sb.append("\n双方确认以上协议内容真实自愿，签字（电子签名）后生效。");
        return sb.toString();
    }

    /** 模拟人脸核验：采集姓名+证件号后四位，返回模拟得分（>=80通过） */
    @Transactional
    public FaceVerification faceVerify(Long caseId, Map<String, Object> body, User operator) {
        MediationCase c = caseRepository.findById(caseId).orElseThrow(() -> new BizException("案件不存在"));
        String party = (String) body.getOrDefault("party", "APPLICANT");
        String name = (String) body.getOrDefault("partyName", "");
        String tail = (String) body.getOrDefault("idCardTail", "");
        if (tail == null || tail.length() < 4) {
            throw new BizException("请输入身份证号后四位");
        }
        // 演示：以证件号后四位做确定性模拟分数（5/6 开头给 90+，保证可演示通过；0000 演示不通过）
        int score;
        if ("0000".equals(tail)) {
            score = 62;
        } else {
            int n = Integer.parseInt(tail);
            score = 85 + (n % 15); // 85-99
        }
        FaceVerification fv = new FaceVerification();
        fv.setCaseId(caseId);
        fv.setParty(party);
        fv.setPartyName(name);
        fv.setIdCardTail(tail);
        fv.setScore(score);
        fv.setPassed(score >= 80);
        fv.setNote(fv.getPassed() ? "活体检测+人脸比对通过" : "比对未达阈值，请重新核验或转线下签署");
        faceRepository.save(fv);

        timelineService.record(caseId, "FACE",
                (name.isBlank() ? party : name) + "人脸核验" + (fv.getPassed() ? "通过" : "未通过"),
                "核验方式：活体检测+实名信息比对；相似度得分：" + score + "；结论：" + fv.getNote(),
                operator);
        return fv;
    }

    /** 当事人线上签署协议（需本人人脸核验通过） */
    @Transactional
    public MediationAgreement sign(Long caseId, String party, User operator) {
        MediationCase c = caseRepository.findById(caseId).orElseThrow(() -> new BizException("案件不存在"));
        MediationAgreement a = agreementRepository.findByCaseId(caseId)
                .orElseThrow(() -> new BizException("协议尚未起草，无法签署"));
        List<FaceVerification> faces = faceRepository.findByCaseIdOrderByCreatedAtDesc(caseId);
        boolean faceOk = faces.stream().anyMatch(FaceVerification::getPassed);
        if (!faceOk) {
            throw new BizException("请先完成本人人脸核验后再签署");
        }
        a.setFaceVerified(true);
        if (a.getFaceVerifiedAt() == null) a.setFaceVerifiedAt(LocalDateTime.now());

        LocalDateTime now = LocalDateTime.now();
        String who;
        if ("RESPONDENT".equals(party)) {
            if (Boolean.TRUE.equals(a.getRespondentSigned())) throw new BizException("被申请人已签署，请勿重复操作");
            a.setRespondentSigned(true);
            a.setRespondentSignedAt(now);
            who = "被申请人" + c.getRespondentName();
        } else {
            if (Boolean.TRUE.equals(a.getApplicantSigned())) throw new BizException("申请人已签署，请勿重复操作");
            a.setApplicantSigned(true);
            a.setApplicantSignedAt(now);
            who = "申请人" + c.getApplicantName();
        }
        if (Boolean.TRUE.equals(a.getApplicantSigned()) && Boolean.TRUE.equals(a.getRespondentSigned())) {
            a.setEffectiveAt(now);
            if (Boolean.TRUE.equals(a.getJudicialConfirmRequested())) {
                c.setStatus(CaseStatus.JUDICIAL_CONFIRMING);
            } else {
                c.setStatus(CaseStatus.PERFORMING);
            }
        }
        agreementRepository.save(a);
        caseRepository.save(c);

        timelineService.record(caseId, "SIGN", who + "完成线上签署",
                "电子签名时间：" + now + "；签署前已完成人脸核验。"
                        + (a.getEffectiveAt() != null ? "双方签署完毕，协议生效。" : "等待另一方签署。"),
                operator);
        return a;
    }
}
