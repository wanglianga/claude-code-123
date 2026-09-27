package com.court.mediation.service;

import com.court.mediation.common.BizException;
import com.court.mediation.dto.CaseDetail;
import com.court.mediation.entity.*;
import com.court.mediation.repo.*;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class CaseService {

    private final CaseRepository caseRepository;
    private final UserRepository userRepository;
    private final EvidenceRepository evidenceRepository;
    private final ScheduleRepository scheduleRepository;
    private final MediationRecordRepository mediationRecordRepository;
    private final AgreementRepository agreementRepository;
    private final PaymentPlanRepository paymentPlanRepository;
    private final SignatureRepository signatureRepository;
    private final ServiceRecordRepository serviceRepository;
    private final VisitRecordRepository visitRepository;
    private final TransferRequestRepository transferRepository;
    private final JudicialConfirmRepository judicialConfirmRepository;
    private final TimelineRepository timelineRepository;
    private final TimelineService timelineService;

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final Map<String, AtomicInteger> caseNoSeq = new ConcurrentHashMap<>();

    /** 列表多条件查询：状态/案由/调解员/关键字/紧急程度/当事人 */
    public List<CaseEntity> search(String status, String causeType, Long mediatorId,
                                   String urgency, String keyword, Boolean unreachable,
                                   Boolean evidenceMissing, Long myApplicantUserId) {
        Specification<CaseEntity> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (status != null && !status.isBlank()) ps.add(cb.equal(root.get("status"), status));
            if (causeType != null && !causeType.isBlank()) ps.add(cb.equal(root.get("causeType"), causeType));
            if (mediatorId != null) ps.add(cb.equal(root.get("mediatorId"), mediatorId));
            if (urgency != null && !urgency.isBlank()) ps.add(cb.equal(root.get("urgency"), urgency));
            if (unreachable != null) ps.add(cb.equal(root.get("respondentUnreachable"), unreachable));
            if (evidenceMissing != null) ps.add(cb.equal(root.get("evidenceMissing"), evidenceMissing));
            if (myApplicantUserId != null) ps.add(cb.equal(root.get("applicantUserId"), myApplicantUserId));
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim() + "%";
                ps.add(cb.or(
                        cb.like(root.get("caseNo"), like),
                        cb.like(root.get("title"), like),
                        cb.like(root.get("applicantName"), like),
                        cb.like(root.get("respondentName"), like)));
            }
            query.orderBy(cb.desc(root.get("createdAt")));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return caseRepository.findAll(spec);
    }

    @Transactional
    public CaseEntity create(CaseEntity c) {
        if (c.getCauseType() == null || c.getCauseType().isBlank()) {
            throw new BizException("案由不能为空");
        }
        if (c.getApplicantName() == null || c.getApplicantName().isBlank()) {
            throw new BizException("申请人姓名不能为空");
        }
        c.setId(null);
        c.setStatus("PENDING_TRIAGE");
        c.setRespondentUnreachable(false);
        c.setEvidenceMissing(false);
        c.setRescheduleCount(0);
        c.setCaseNo(generateCaseNo());
        CaseEntity saved = caseRepository.save(c);

        // 证据清单文本拆分为明细条目（便于缺料登记）
        if (c.getEvidenceSummary() != null && !c.getEvidenceSummary().isBlank()) {
            for (String line : c.getEvidenceSummary().split("[;；\n]")) {
                String name = line.trim();
                if (!name.isEmpty()) {
                    Evidence e = new Evidence();
                    e.setCaseId(saved.getId());
                    e.setName(name);
                    e.setEvidenceType("书证");
                    e.setSubmittedBy(saved.getApplicantUserId());
                    evidenceRepository.save(e);
                }
            }
        }
        timelineService.record(saved.getId(), "CREATED",
                "当事人/窗口提交纠纷申请：" + saved.getTitle()
                        + "，诉求金额 " + saved.getClaimAmount()
                        + " 元，愿意线上调解：" + boolText(saved.getWillingOnline())
                        + "，紧急程度：" + urgencyText(saved.getUrgency()));
        return saved;
    }

    /** 立案窗口分流：指定调解员、调解方式，进入调解中心待排期 */
    @Transactional
    public CaseEntity triage(Long caseId, Long mediatorId, String mediationMode, String remark) {
        CaseEntity c = getById(caseId);
        if (!"PENDING_TRIAGE".equals(c.getStatus()) && !"PENDING_SCHEDULE".equals(c.getStatus())) {
            throw new BizException("当前状态（" + statusText(c.getStatus()) + "）不能分流");
        }
        User mediator = userRepository.findById(mediatorId)
                .filter(u -> "MEDIATOR".equals(u.getRole()))
                .orElseThrow(() -> new BizException("请选择有效的调解员"));
        c.setMediatorId(mediatorId);
        c.setMediationMode(mediationMode);
        c.setTriageUserId(com.court.mediation.security.CurrentUser.userId());
        c.setTriageRemark(remark);
        c.setStatus("PENDING_SCHEDULE");
        caseRepository.save(c);
        timelineService.record(caseId, "TRIAGED",
                "立案窗口完成分流：指派调解员 " + mediator.getRealName()
                        + "（专长：" + nz(mediator.getSpecialties()) + "），调解方式："
                        + ("ONLINE".equals(mediationMode) ? "线上" : "线下")
                        + "。分流意见：" + nz(remark));
        return c;
    }

    public CaseEntity getById(Long id) {
        return caseRepository.findById(id).orElseThrow(() -> new BizException("案件不存在"));
    }

    @Transactional(readOnly = true)
    public CaseDetail detail(Long id) {
        CaseEntity c = getById(id);
        User mediator = c.getMediatorId() == null ? null
                : userRepository.findById(c.getMediatorId()).orElse(null);
        Agreement active = agreementRepository.findByCaseIdAndStatus(id, "SIGNED").orElse(null);
        return new CaseDetail(
                c, mediator,
                evidenceRepository.findByCaseIdOrderByCreatedAtAsc(id),
                scheduleRepository.findByCaseIdOrderByVersionDesc(id),
                mediationRecordRepository.findByCaseIdOrderBySessionTimeAsc(id),
                active,
                agreementRepository.findByCaseIdOrderByCreatedAtDesc(id),
                paymentPlanRepository.findByCaseIdOrderByPeriodNoAsc(id),
                active == null ? List.of() : signatureRepository.findByAgreementIdOrderBySignedAtAsc(active.getId()),
                serviceRepository.findByCaseIdOrderByCreatedAtDesc(id),
                visitRepository.findByCaseIdOrderByVisitTimeDesc(id),
                transferRepository.findByCaseIdOrderByCreatedAtDesc(id),
                judicialConfirmRepository.findByCaseIdOrderByCreatedAtDesc(id),
                timelineRepository.findByCaseIdOrderByCreatedAtAscIdAsc(id)
        );
    }

    @Transactional
    public CaseEntity updateStatus(Long caseId, String status, String reason) {
        CaseEntity c = getById(caseId);
        String old = c.getStatus();
        c.setStatus(status);
        if (List.of("COMPLETED", "FAILED", "TRANSFERRED").contains(status)) {
            c.setClosedAt(java.time.LocalDateTime.now());
        }
        caseRepository.save(c);
        timelineService.record(caseId, "STATUS_CHANGED",
                "案件状态由【" + statusText(old) + "】变更为【" + statusText(status) + "】"
                        + (reason == null || reason.isBlank() ? "" : "，原因：" + reason));
        return c;
    }

    private String generateCaseNo() {
        String day = LocalDate.now().format(NO_FMT);
        int seq = caseNoSeq.computeIfAbsent(day, k -> new AtomicInteger(0)).incrementAndGet();
        return "诉前调（" + LocalDate.now().getYear() + "）预字第" + day.substring(4) + String.format("%03d", seq) + "号";
    }

    public static String statusText(String s) {
        return switch (s) {
            case "PENDING_TRIAGE" -> "待分流";
            case "PENDING_SCHEDULE" -> "待排期";
            case "SCHEDULED" -> "已排期";
            case "MEDIATING" -> "调解中";
            case "AGREED" -> "已达成协议";
            case "PERFORMING" -> "履行中";
            case "COMPLETED" -> "已履行完毕";
            case "FAILED" -> "调解不成";
            case "TRANSFERRED" -> "已转立案";
            default -> s;
        };
    }

    public static String urgencyText(String s) {
        return switch (s == null ? "" : s) {
            case "URGENT" -> "紧急";
            case "VERY_URGENT" -> "特急";
            default -> "普通";
        };
    }

    private static String boolText(Boolean b) {
        return Boolean.TRUE.equals(b) ? "是" : "否";
    }

    private static String nz(String s) {
        return s == null || s.isBlank() ? "无" : s;
    }
}
