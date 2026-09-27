package cn.court.mediation.controller;

import cn.court.mediation.common.ApiResponse;
import cn.court.mediation.entity.PerformancePlan;
import cn.court.mediation.security.CurrentUser;
import cn.court.mediation.service.PerformanceService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PerformanceController {

    private final PerformanceService performanceService;

    public PerformanceController(PerformanceService performanceService) {
        this.performanceService = performanceService;
    }

    /** 依据协议生成履行节点（分期明细一次性提交） */
    @PostMapping("/cases/{id}/plans")
    public ApiResponse<List<PerformancePlan>> generate(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                                       HttpServletRequest request) {
        return ApiResponse.ok(performanceService.generatePlans(id, body, CurrentUser.get(request)));
    }

    /** 执行联络/调解员确认到账（支持部分履行） */
    @PostMapping("/plans/{id}/pay")
    public ApiResponse<PerformancePlan> pay(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                            HttpServletRequest request) {
        return ApiResponse.ok(performanceService.confirmPayment(id, body, CurrentUser.get(request)));
    }

    /** 手动触发逾期扫描（演示用） */
    @PostMapping("/overdue/scan")
    public ApiResponse<Map<String, Object>> scanNow() {
        int n = performanceService.runOverdueScanNow();
        return ApiResponse.ok(Map.of("newlyOverdue", n));
    }
}
