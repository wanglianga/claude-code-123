package com.court.mediation.controller;

import com.court.mediation.common.R;
import com.court.mediation.dto.CaseDetail;
import com.court.mediation.entity.CaseEntity;
import com.court.mediation.entity.User;
import com.court.mediation.security.CurrentUser;
import com.court.mediation.service.CaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cases")
@RequiredArgsConstructor
public class CaseController {

    private final CaseService caseService;

    @GetMapping
    public R<List<CaseEntity>> list(@RequestParam(required = false) String status,
                                    @RequestParam(required = false) String causeType,
                                    @RequestParam(required = false) Long mediatorId,
                                    @RequestParam(required = false) String urgency,
                                    @RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) Boolean unreachable,
                                    @RequestParam(required = false) Boolean evidenceMissing,
                                    @RequestParam(required = false) boolean onlyMine) {
        Long myUserId = null;
        if (onlyMine) {
            User me = CurrentUser.get();
            if ("PARTY".equals(me.getRole())) myUserId = me.getId();
        }
        return R.ok(caseService.search(status, causeType, mediatorId, urgency, keyword,
                unreachable, evidenceMissing, myUserId));
    }

    @GetMapping("/{id}")
    public R<CaseDetail> detail(@PathVariable Long id) {
        return R.ok(caseService.detail(id));
    }

    /** 当事人/窗口新建案件 */
    @PostMapping
    public R<CaseEntity> create(@RequestBody CaseEntity c) {
        User me = CurrentUser.get();
        if ("PARTY".equals(me.getRole())) {
            c.setApplicantUserId(me.getId());
            if (c.getApplicantName() == null || c.getApplicantName().isBlank())
                c.setApplicantName(me.getRealName());
            if (c.getApplicantPhone() == null || c.getApplicantPhone().isBlank())
                c.setApplicantPhone(me.getPhone());
        }
        return R.ok(caseService.create(c));
    }

    /** 立案窗口分流 */
    @PostMapping("/{id}/triage")
    public R<CaseEntity> triage(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long mediatorId = Long.valueOf(String.valueOf(body.get("mediatorId")));
        String mode = String.valueOf(body.getOrDefault("mediationMode", "ONLINE"));
        String remark = body.get("remark") == null ? null : String.valueOf(body.get("remark"));
        return R.ok(caseService.triage(id, mediatorId, mode, remark));
    }

    /** 通用状态变更（调解不成/挂起恢复等） */
    @PostMapping("/{id}/status")
    public R<CaseEntity> status(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return R.ok(caseService.updateStatus(id, body.get("status"), body.get("reason")));
    }
}
