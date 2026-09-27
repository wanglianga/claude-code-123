package com.court.mediation.service;

import com.court.mediation.common.BizException;
import com.court.mediation.entity.CaseEntity;
import com.court.mediation.entity.PaymentPlan;
import com.court.mediation.repo.CaseRepository;
import com.court.mediation.repo.PaymentPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 协议履行服务：执行联络人员登记收款（支持部分履行）、催办；
 * 每日定时扫描把到期未付节点标记为逾期；全部节点付清后案件履行完毕。
 */
@Service
@RequiredArgsConstructor
public class PerformanceService {

    private final PaymentPlanRepository planRepository;
    private final CaseRepository caseRepository;
    private final TimelineService timelineService;

    /** 登记收款：paidAmount 为本次实付，可小于应付（部分履行） */
    @Transactional
    public PaymentPlan confirmPayment(Long planId, BigDecimal paidAmount, String remark) {
        PaymentPlan p = planRepository.findById(planId).orElseThrow(() -> new BizException("履行节点不存在"));
        if (paidAmount == null || paidAmount.signum() <= 0) throw new BizException("实收金额必须大于0");

        BigDecimal total = nz(p.getPaidAmount()).add(paidAmount);
        p.setPaidAmount(total);
        p.setConfirmedBy(com.court.mediation.security.CurrentUser.userId());
        if (remark != null) p.setRemark(remark);
        if (total.compareTo(p.getAmount()) >= 0) {
            p.setStatus("PAID");
            p.setPaidAt(java.time.LocalDateTime.now());
        } else {
            p.setStatus("PARTIAL");
        }
        PaymentPlan saved = planRepository.save(p);

        timelineService.record(p.getCaseId(), "PAYMENT_PAID",
                "确认第 " + p.getPeriodNo() + " 期收款 " + paidAmount
                        + " 元，节点应付 " + p.getAmount() + " 元，累计已付 " + total + " 元，"
                        + ("PAID".equals(p.getStatus()) ? "该期已履行完毕。" : "属部分履行，尚欠 "
                        + p.getAmount().subtract(total) + " 元。")
                        + (remark == null ? "" : " 备注：" + remark), p.getId());

        // 判断协议是否全部履行完毕
        List<PaymentPlan> all = planRepository.findByAgreementIdOrderByPeriodNoAsc(p.getAgreementId());
        boolean allPaid = all.stream().allMatch(x -> "PAID".equals(x.getStatus()));
        CaseEntity c = caseRepository.findById(p.getCaseId()).orElseThrow();
        if (allPaid) {
            if (!"COMPLETED".equals(c.getStatus())) {
                c.setStatus("COMPLETED");
                c.setClosedAt(java.time.LocalDateTime.now());
                caseRepository.save(c);
                timelineService.record(p.getCaseId(), "PAYMENT_PAID",
                        "全部履行节点均已付清，调解协议履行完毕，案件结案归档。");
            }
        } else if (!"PERFORMING".equals(c.getStatus()) && !"COMPLETED".equals(c.getStatus())) {
            c.setStatus("PERFORMING");
            caseRepository.save(c);
        }
        return saved;
    }

    /** 催办（执行联络/调解员） */
    @Transactional
    public PaymentPlan remind(Long planId, String content) {
        PaymentPlan p = planRepository.findById(planId).orElseThrow(() -> new BizException("履行节点不存在"));
        timelineService.record(p.getCaseId(), "REMIND",
                "对第 " + p.getPeriodNo() + " 期（应付日 " + p.getDueDate() + "，应付 "
                        + p.getAmount() + " 元）发起履行催告：" + nz(content)
                        + "。催告记录作为违约/转立案过程材料留档。", p.getId());
        return p;
    }

    /** 每日 08:00 扫描逾期节点 */
    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public int scanOverdue() {
        List<PaymentPlan> due = planRepository.findByStatusAndDueDateBefore("PENDING", LocalDate.now());
        List<PaymentPlan> partialDue = planRepository.findByStatusAndDueDateBefore("PARTIAL", LocalDate.now());
        due.addAll(partialDue);
        for (PaymentPlan p : due) {
            p.setStatus("OVERDUE");
            planRepository.save(p);
            timelineService.record(p.getCaseId(), "PAYMENT_OVERDUE",
                    "第 " + p.getPeriodNo() + " 期已超过履行期限（应付日 " + p.getDueDate()
                            + "）未足额付款，系统标记为逾期。", p.getId());
            CaseEntity c = caseRepository.findById(p.getCaseId()).orElse(null);
            if (c != null && !List.of("PERFORMING", "COMPLETED", "TRANSFERRED", "FAILED").contains(c.getStatus())) {
                c.setStatus("PERFORMING");
                caseRepository.save(c);
            }
        }
        return due.size();
    }

    /** 手动触发逾期扫描（演示/补跑） */
    public int scanOverdueManual() {
        return scanOverdue();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static String nz(String s) {
        return s == null || s.isBlank() ? "请按协议尽快履行" : s;
    }
}
