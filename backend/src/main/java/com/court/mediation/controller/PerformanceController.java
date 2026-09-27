package com.court.mediation.controller;

import com.court.mediation.common.R;
import com.court.mediation.entity.PaymentPlan;
import com.court.mediation.service.PerformanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/performance")
@RequiredArgsConstructor
public class PerformanceController {

    private final PerformanceService performanceService;

    /** 执行联络登记收款（支持部分履行） */
    @PostMapping("/plans/{id}/pay")
    public R<PaymentPlan> pay(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        BigDecimal amount = new BigDecimal(String.valueOf(body.get("paidAmount")));
        String remark = body.get("remark") == null ? null : String.valueOf(body.get("remark"));
        return R.ok(performanceService.confirmPayment(id, amount, remark));
    }

    /** 发起催告 */
    @PostMapping("/plans/{id}/remind")
    public R<PaymentPlan> remind(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        return R.ok(performanceService.remind(id, body == null ? null : body.get("content")));
    }

    /** 手动扫描逾期（定时任务每日08:00自动执行） */
    @PostMapping("/scan-overdue")
    public R<Map<String, Object>> scanOverdue() {
        int n = performanceService.scanOverdueManual();
        return R.ok(Map.of("overdueMarked", n));
    }
}
