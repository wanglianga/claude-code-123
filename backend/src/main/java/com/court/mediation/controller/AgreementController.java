package com.court.mediation.controller;

import com.court.mediation.common.R;
import com.court.mediation.entity.Agreement;
import com.court.mediation.entity.PaymentPlan;
import com.court.mediation.entity.Signature;
import com.court.mediation.service.AgreementService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agreements")
@RequiredArgsConstructor
public class AgreementController {

    private final AgreementService agreementService;

    /** 起草协议（附分期履行节点） */
    @PostMapping
    public R<Agreement> draft(@RequestBody Map<String, Object> body) {
        Agreement a = new Agreement();
        a.setCaseId(Long.valueOf(String.valueOf(body.get("caseId"))));
        if (body.get("totalAmount") != null)
            a.setTotalAmount(new java.math.BigDecimal(String.valueOf(body.get("totalAmount"))));
        a.setPaymentMethod(body.get("paymentMethod") == null ? "LUMP_SUM" : String.valueOf(body.get("paymentMethod")));
        a.setGuarantor(body.get("guarantor") == null ? null : String.valueOf(body.get("guarantor")));
        if (body.get("deadline") != null) a.setDeadline(java.time.LocalDate.parse(String.valueOf(body.get("deadline"))));
        a.setApplyJudicialConfirm(Boolean.valueOf(String.valueOf(body.getOrDefault("applyJudicialConfirm", "false"))));
        if (body.get("content") != null) a.setContent(String.valueOf(body.get("content")));

        List<PaymentPlan> plans = new java.util.ArrayList<>();
        Object planObj = body.get("plans");
        if (planObj instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> pm) {
                    PaymentPlan p = new PaymentPlan();
                    p.setDueDate(java.time.LocalDate.parse(String.valueOf(pm.get("dueDate"))));
                    p.setAmount(new java.math.BigDecimal(String.valueOf(pm.get("amount"))));
                    plans.add(p);
                }
            }
        }
        return R.ok(agreementService.draft(a, plans));
    }

    /** 线上签署（人脸核验） */
    @PostMapping("/{id}/sign")
    public R<Signature> sign(@PathVariable Long id, @RequestBody Map<String, Object> body,
                             HttpServletRequest request) {
        String role = String.valueOf(body.getOrDefault("signerRole", "RESPONDENT"));
        String name = String.valueOf(body.getOrDefault("signerName", "当事人"));
        Boolean facePass = body.get("facePass") == null ? null : Boolean.valueOf(String.valueOf(body.get("facePass")));
        Double score = body.get("faceScore") == null ? null : Double.valueOf(String.valueOf(body.get("faceScore")));
        String signData = body.get("signData") == null ? null : String.valueOf(body.get("signData"));
        return R.ok(agreementService.sign(id, role, name, facePass, score, signData, request.getRemoteAddr()));
    }
}
