package com.court.mediation.controller;

import com.court.mediation.common.R;
import com.court.mediation.entity.TransferRequest;
import com.court.mediation.service.CaseService;
import com.court.mediation.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;
    private final CaseService caseService;

    /** 一方申请转立案（自动归集连续过程材料） */
    @PostMapping
    public R<TransferRequest> apply(@RequestBody Map<String, String> body) {
        return R.ok(transferService.apply(
                Long.valueOf(body.get("caseId")),
                body.get("applicantName"),
                body.get("reasonCode"),
                body.get("reason")));
    }

    /** 法官助理审核 */
    @PostMapping("/{id}/review")
    public R<TransferRequest> review(@PathVariable Long id, @RequestBody Map<String, String> body) {
        boolean accept = Boolean.parseBoolean(body.getOrDefault("accept", "true"));
        return R.ok(transferService.review(id, accept, body.get("filingNo"), body.get("opinion")));
    }

    /** 导出连续过程材料包（文本，可打印附卷） */
    @GetMapping("/cases/{caseId}/material-package")
    public ResponseEntity<byte[]> materialPackage(@PathVariable Long caseId) {
        String text = transferService.buildMaterialPackage(caseService.getById(caseId));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8));
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename*=UTF-8''case-" + caseId + "-materials.txt");
        return new ResponseEntity<>(text.getBytes(StandardCharsets.UTF_8), headers, 200);
    }
}
