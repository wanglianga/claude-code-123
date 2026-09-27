package com.court.mediation.controller;

import com.court.mediation.common.R;
import com.court.mediation.entity.CaseEntity;
import com.court.mediation.entity.Evidence;
import com.court.mediation.entity.MediationRecord;
import com.court.mediation.service.MediationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mediation")
@RequiredArgsConstructor
public class MediationController {

    private final MediationService mediationService;

    /** 调解员录入调解记录 */
    @PostMapping("/records")
    public R<MediationRecord> saveRecord(@RequestBody MediationRecord record) {
        return R.ok(mediationService.saveRecord(record));
    }

    /** 登记被申请人失联 */
    @PostMapping("/cases/{id}/unreachable")
    public R<CaseEntity> unreachable(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        return R.ok(mediationService.markUnreachable(id, body == null ? null : body.get("remark")));
    }

    /** 恢复联系 */
    @PostMapping("/cases/{id}/reached")
    public R<CaseEntity> reached(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        return R.ok(mediationService.markReached(id, body == null ? null : body.get("remark")));
    }

    /** 登记证据缺失 */
    @PostMapping("/cases/{id}/evidence-missing")
    public R<CaseEntity> evidenceMissing(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        return R.ok(mediationService.markEvidenceMissing(id, body == null ? null : body.get("remark")));
    }

    /** 补充证据 */
    @PostMapping("/evidence")
    public R<Evidence> addEvidence(@RequestBody Evidence evidence) {
        return R.ok(mediationService.addEvidence(evidence));
    }
}
