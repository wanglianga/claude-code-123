package cn.court.mediation.controller;

import cn.court.mediation.common.ApiResponse;
import cn.court.mediation.dto.CaseCreateRequest;
import cn.court.mediation.entity.MediationCase;
import cn.court.mediation.security.CurrentUser;
import cn.court.mediation.service.CaseService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cases")
public class CaseController {

    private final CaseService caseService;

    public CaseController(CaseService caseService) {
        this.caseService = caseService;
    }

    @PostMapping
    public ApiResponse<MediationCase> create(@RequestBody CaseCreateRequest req, HttpServletRequest request) {
        return ApiResponse.ok(caseService.create(req, CurrentUser.get(request)));
    }

    @GetMapping
    public ApiResponse<List<MediationCase>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        return ApiResponse.ok(caseService.list(status, type, keyword, CurrentUser.get(request)));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.ok(caseService.detail(id));
    }

    /** 窗口分流 */
    @PostMapping("/{id}/triage")
    public ApiResponse<MediationCase> triage(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                             HttpServletRequest request) {
        return ApiResponse.ok(caseService.triage(id, body, CurrentUser.get(request)));
    }

    /** 失联登记/恢复 */
    @PostMapping("/{id}/missing")
    public ApiResponse<Void> markMissing(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                         HttpServletRequest request) {
        caseService.markMissing(id, Boolean.TRUE.equals(body.get("missing")),
                (String) body.get("note"), CurrentUser.get(request));
        return ApiResponse.ok();
    }

    /** 证据补正 */
    @PostMapping("/{id}/evidence")
    public ApiResponse<Void> supplement(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                        HttpServletRequest request) {
        caseService.supplementEvidence(id, (String) body.get("note"), CurrentUser.get(request));
        return ApiResponse.ok();
    }
}
