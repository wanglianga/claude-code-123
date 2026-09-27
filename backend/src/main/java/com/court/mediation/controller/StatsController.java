package com.court.mediation.controller;

import com.court.mediation.common.R;
import com.court.mediation.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    /** 法院决策看板：调解效率/协议履行/转立案原因 */
    @GetMapping("/overview")
    public R<Map<String, Object>> overview() {
        return R.ok(statsService.overview());
    }

    /** 预警：逾期、失联、缺料、多次改期 */
    @GetMapping("/alerts")
    public R<Map<String, Object>> alerts() {
        return R.ok(statsService.alerts());
    }
}
