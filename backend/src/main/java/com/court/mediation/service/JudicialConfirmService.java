package com.court.mediation.service;

import com.court.mediation.common.BizException;
import com.court.mediation.entity.Agreement;
import com.court.mediation.entity.CaseEntity;
import com.court.mediation.entity.JudicialConfirm;
import com.court.mediation.repo.AgreementRepository;
import com.court.mediation.repo.CaseRepository;
import com.court.mediation.repo.JudicialConfirmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** 司法确认：根据已生效调解协议一键生成司法确认申请材料，提交、裁定确认 */
@Service
@RequiredArgsConstructor
public class JudicialConfirmService {

    private final JudicialConfirmRepository repository;
    private final AgreementRepository agreementRepository;
    private final CaseRepository caseRepository;
    private final TimelineService timelineService;

    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("yyyy年M月d日");

    @Transactional
    public JudicialConfirm generate(Long agreementId) {
        Agreement a = agreementRepository.findById(agreementId).orElseThrow(() -> new BizException("协议不存在"));
        CaseEntity c = caseRepository.findById(a.getCaseId()).orElseThrow(() -> new BizException("案件不存在"));

        JudicialConfirm jc = new JudicialConfirm();
        jc.setCaseId(c.getId());
        jc.setAgreementId(a.getId());
        jc.setConfirmNo("确字" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%04d", c.getId()));
        jc.setPartyInfo("申请人（原纠纷申请人）：" + nz(c.getApplicantName())
                + "，联系电话：" + nz(c.getApplicantPhone()) + "\n"
                + "被申请人（原纠纷被申请人）：" + nz(c.getRespondentName())
                + "，联系电话：" + nz(c.getRespondentPhone()) + "，住址：" + nz(c.getRespondentAddress()));
        jc.setClaims("请求人民法院依法确认申请人与被申请人于 " + LocalDate.now().format(D)
                + " 在本院诉前调解中心达成的调解协议（" + a.getAgreementNo() + "）有效，"
                + "并裁定被申请人按协议约定支付款项合计人民币 " + a.getTotalAmount() + " 元。");
        jc.setMaterials("1. 司法确认申请书（双方共同申请）；\n"
                + "2. 调解协议书（" + a.getAgreementNo() + "，含双方电子签名及人脸核验记录）；\n"
                + "3. 申请人、被申请人身份证明；\n"
                + "4. 纠纷相关证据材料及调解笔录；\n"
                + "5. 电子送达与签署过程记录。");
        jc.setStatus("DRAFT");
        jc.setCreatedBy(com.court.mediation.security.CurrentUser.userId());
        JudicialConfirm saved = repository.save(jc);

        timelineService.record(c.getId(), "JUDICIAL_APPLY",
                "根据已生效协议 " + a.getAgreementNo() + " 生成司法确认申请材料（"
                        + saved.getConfirmNo() + "），包含申请书、身份证明清单、协议及签署核验记录。",
                saved.getId());
        return saved;
    }

    @Transactional
    public JudicialConfirm submit(Long id) {
        JudicialConfirm jc = repository.findById(id).orElseThrow(() -> new BizException("司法确认材料不存在"));
        jc.setStatus("SUBMITTED");
        JudicialConfirm saved = repository.save(jc);
        timelineService.record(jc.getCaseId(), "JUDICIAL_APPLY",
                "司法确认申请 " + jc.getConfirmNo() + " 已提交人民法院审查。", jc.getId());
        return saved;
    }

    @Transactional
    public JudicialConfirm rule(Long id, boolean confirm, String rulingNo) {
        JudicialConfirm jc = repository.findById(id).orElseThrow(() -> new BizException("司法确认材料不存在"));
        jc.setStatus(confirm ? "CONFIRMED" : "REJECTED");
        jc.setRulingNo(rulingNo);
        JudicialConfirm saved = repository.save(jc);
        timelineService.record(jc.getCaseId(), "JUDICIAL_CONFIRMED",
                (confirm ? "人民法院裁定确认调解协议有效，裁定书号：" + rulingNo
                        : "人民法院不予确认，裁定：" + rulingNo), jc.getId());
        return saved;
    }

    private static String nz(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }
}
