package com.court.mediation.controller;

import com.court.mediation.common.R;
import com.court.mediation.entity.JudicialConfirm;
import com.court.mediation.service.JudicialConfirmService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/judicial")
@RequiredArgsConstructor
public class JudicialConfirmController {

    private final JudicialConfirmService service;

    /** 根据协议生成司法确认申请材料 */
    @PostMapping("/generate/{agreementId}")
    public R<JudicialConfirm> generate(@PathVariable Long agreementId) {
        return R.ok(service.generate(agreementId));
    }

    @PostMapping("/{id}/submit")
    public R<JudicialConfirm> submit(@PathVariable Long id) {
        return R.ok(service.submit(id));
    }

    @PostMapping("/{id}/rule")
    public R<JudicialConfirm> rule(@PathVariable Long id, @RequestBody Map<String, String> body) {
        boolean confirm = Boolean.parseBoolean(body.getOrDefault("confirm", "true"));
        return R.ok(service.rule(id, confirm, body.get("rulingNo")));
    }
}
