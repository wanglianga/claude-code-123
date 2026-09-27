package com.court.mediation.service;

import com.court.mediation.entity.*;
import com.court.mediation.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 统计分析：调解效率（分流/平均周期/调解员负载）、协议履行（金额/逾期/部分履行）、
 * 转立案原因分布，供法院决策看板使用。
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final CaseRepository caseRepository;
    private final UserRepository userRepository;
    private final AgreementRepository agreementRepository;
    private final PaymentPlanRepository planRepository;
    private final TransferRequestRepository transferRepository;
    private final MediationRecordRepository recordRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> overview() {
        List<CaseEntity> all = caseRepository.findAll();
        List<Agreement> agreements = agreementRepository.findAll();
        List<PaymentPlan> plans = planRepository.findAll();
        List<TransferRequest> transfers = transferRepository.findAll();

        Map<String, Object> m = new LinkedHashMap<>();

        // 一、核心指标
        long total = all.size();
        long pending = all.stream().filter(c -> List.of("PENDING_TRIAGE", "PENDING_SCHEDULE").contains(c.getStatus())).count();
        long mediating = all.stream().filter(c -> List.of("SCHEDULED", "MEDIATING").contains(c.getStatus())).count();
        long agreed = agreements.size();
        long completed = all.stream().filter(c -> "COMPLETED".equals(c.getStatus())).count();
        long transferred = all.stream().filter(c -> "TRANSFERRED".equals(c.getStatus())).count();
        long failed = all.stream().filter(c -> "FAILED".equals(c.getStatus())).count();

        Map<String, Object> kpi = new LinkedHashMap<>();
        kpi.put("totalCases", total);
        kpi.put("pendingCases", pending);
        kpi.put("mediatingCases", mediating);
        kpi.put("agreementCount", agreed);
        kpi.put("completedCount", completed);
        kpi.put("transferredCount", transferred);
        kpi.put("failedCount", failed);
        kpi.put("agreementRate", total == 0 ? 0 : Math.round(agreed * 10000.0 / total) / 100.0);
        kpi.put("performRate", agreed == 0 ? 0 : Math.round(completed * 10000.0 / agreed) / 100.0);
        kpi.put("transferRate", total == 0 ? 0 : Math.round(transferred * 10000.0 / total) / 100.0);
        m.put("kpi", kpi);

        // 二、按案由统计
        Map<String, long[]> byCause = new LinkedHashMap<>();
        for (String cause : List.of("民间借贷", "物业费", "交通事故", "邻里纠纷", "小额买卖合同")) {
            byCause.put(cause, new long[3]); // 总数/达成协议/转立案
        }
        Set<Long> agreedCaseIds = new HashSet<>();
        for (Agreement a : agreements) agreedCaseIds.add(a.getCaseId());
        Set<Long> transferredCaseIds = new HashSet<>();
        for (TransferRequest t : transfers) if ("ACCEPTED".equals(t.getStatus())) transferredCaseIds.add(t.getCaseId());
        for (CaseEntity c : all) {
            long[] arr = byCause.computeIfAbsent(c.getCauseType(), k -> new long[3]);
            arr[0]++;
            if (agreedCaseIds.contains(c.getId())) arr[1]++;
            if (transferredCaseIds.contains(c.getId())) arr[2]++;
        }
        List<Map<String, Object>> causeStats = new ArrayList<>();
        byCause.forEach((cause, arr) -> {
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("cause", cause);
            x.put("total", arr[0]);
            x.put("agreed", arr[1]);
            x.put("transferred", arr[2]);
            causeStats.add(x);
        });
        m.put("byCause", causeStats);

        // 三、调解效率：状态分布 + 平均调解周期（达成协议案件：创建→协议签署）
        Map<String, Long> statusDist = new LinkedHashMap<>();
        for (String s : List.of("PENDING_TRIAGE", "PENDING_SCHEDULE", "SCHEDULED", "MEDIATING",
                "AGREED", "PERFORMING", "COMPLETED", "FAILED", "TRANSFERRED")) {
            statusDist.put(CaseService.statusText(s), all.stream().filter(c -> s.equals(c.getStatus())).count());
        }
        m.put("statusDist", statusDist);

        List<Double> days = new ArrayList<>();
        for (Agreement a : agreements) {
            CaseEntity c = all.stream().filter(x -> x.getId().equals(a.getCaseId())).findFirst().orElse(null);
            if (c != null && a.getSignedAt() != null && c.getCreatedAt() != null) {
                days.add(ChronoUnit.HOURS.between(c.getCreatedAt(), a.getSignedAt()) / 24.0);
            }
        }
        double avgDays = days.isEmpty() ? 0 : days.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        m.put("avgMediationDays", Math.round(avgDays * 10) / 10.0);

        // 四、协议履行：金额与逾期
        double totalDue = plans.stream().mapToDouble(p -> p.getAmount() == null ? 0 : p.getAmount().doubleValue()).sum();
        double totalPaid = plans.stream().mapToDouble(p -> p.getPaidAmount() == null ? 0 : p.getPaidAmount().doubleValue()).sum();
        long overdue = plans.stream().filter(p -> "OVERDUE".equals(p.getStatus())).count();
        long partial = plans.stream().filter(p -> "PARTIAL".equals(p.getStatus())).count();
        long paidPlans = plans.stream().filter(p -> "PAID".equals(p.getStatus())).count();
        Map<String, Object> perf = new LinkedHashMap<>();
        perf.put("planCount", plans.size());
        perf.put("paidPlanCount", paidPlans);
        perf.put("overdueCount", overdue);
        perf.put("partialCount", partial);
        perf.put("totalDue", Math.round(totalDue * 100) / 100.0);
        perf.put("totalPaid", Math.round(totalPaid * 100) / 100.0);
        perf.put("paidRate", totalDue == 0 ? 0 : Math.round(totalPaid * 10000.0 / totalDue) / 100.0);
        m.put("performance", perf);

        // 五、转立案原因分布
        Map<String, Long> reasonDist = new LinkedHashMap<>();
        for (TransferRequest t : transfers) {
            String label = TransferService.reasonText(t.getReasonCode());
            reasonDist.merge(label, 1L, Long::sum);
        }
        m.put("transferReasons", reasonDist);

        // 六、调解员工作量
        List<Map<String, Object>> mediatorStats = new ArrayList<>();
        List<User> mediators = userRepository.findByRoleAndEnabledTrue("MEDIATOR");
        for (User u : mediators) {
            List<CaseEntity> mine = all.stream().filter(c -> u.getId().equals(c.getMediatorId())).toList();
            long mineAgreed = mine.stream().filter(c -> agreedCaseIds.contains(c.getId())).count();
            long mineTransferred = mine.stream().filter(c -> transferredCaseIds.contains(c.getId())).count();
            long unresolved = mine.stream().filter(c -> List.of("SCHEDULED", "MEDIATING", "AGREED", "PERFORMING").contains(c.getStatus())).count();
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("mediator", u.getRealName());
            x.put("specialties", u.getSpecialties());
            x.put("total", mine.size());
            x.put("agreed", mineAgreed);
            x.put("transferred", mineTransferred);
            x.put("unresolved", unresolved);
            mediatorStats.add(x);
        }
        m.put("mediatorStats", mediatorStats);

        return m;
    }

    /** 逾期/失联/缺料预警清单 */
    @Transactional(readOnly = true)
    public Map<String, Object> alerts() {
        List<PaymentPlan> overdue = planRepository.findAll().stream()
                .filter(p -> "OVERDUE".equals(p.getStatus())).toList();
        List<CaseEntity> unreachable = caseRepository.findAll().stream()
                .filter(c -> Boolean.TRUE.equals(c.getRespondentUnreachable())
                        && !List.of("TRANSFERRED", "COMPLETED").contains(c.getStatus())).toList();
        List<CaseEntity> missing = caseRepository.findAll().stream()
                .filter(c -> Boolean.TRUE.equals(c.getEvidenceMissing())).toList();
        List<CaseEntity> rescheduled = caseRepository.findAll().stream()
                .filter(c -> c.getRescheduleCount() != null && c.getRescheduleCount() >= 2
                        && !List.of("TRANSFERRED", "COMPLETED").contains(c.getStatus())).toList();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("overdueCount", overdue.size());
        m.put("unreachableCount", unreachable.size());
        m.put("missingCount", missing.size());
        m.put("rescheduledCount", rescheduled.size());
        m.put("unreachableCases", brief(unreachable));
        m.put("missingCases", brief(missing));
        m.put("rescheduledCases", brief(rescheduled));
        return m;
    }

    private List<Map<String, Object>> brief(List<CaseEntity> list) {
        return list.stream().map(c -> {
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("id", c.getId());
            x.put("caseNo", c.getCaseNo());
            x.put("title", c.getTitle());
            x.put("status", CaseService.statusText(c.getStatus()));
            x.put("rescheduleCount", c.getRescheduleCount());
            return x;
        }).toList();
    }
}
