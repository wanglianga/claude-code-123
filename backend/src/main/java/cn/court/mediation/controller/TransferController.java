package cn.court.mediation.controller;

import cn.court.mediation.common.ApiResponse;
import cn.court.mediation.entity.MediationCase;
import cn.court.mediation.entity.TransferMaterial;
import cn.court.mediation.security.CurrentUser;
import cn.court.mediation.service.TransferService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/cases/{id}")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    /** 生成转立案材料包（自动汇编全过程连续材料） */
    @PostMapping("/transfer")
    public ApiResponse<TransferMaterial> prepare(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                                 HttpServletRequest request) {
        return ApiResponse.ok(transferService.prepareTransfer(id, body, CurrentUser.get(request)));
    }

    /** 窗口接收并正式转立案 */
    @PostMapping("/transfer/accept")
    public ApiResponse<MediationCase> accept(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                             HttpServletRequest request) {
        return ApiResponse.ok(transferService.acceptTransfer(id,
                (String) body.getOrDefault("litigationCaseNo", "MS" + id + "号"),
                CurrentUser.get(request)));
    }

    /** 法官审查通过司法确认，转入履行阶段 */
    @PostMapping("/judicial-confirm/pass")
    public ApiResponse<MediationCase> passJudicial(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.ok(transferService.passJudicialConfirm(id, CurrentUser.get(request)));
    }

    /** 下载司法确认申请书（txt） */
    @GetMapping("/judicial-confirm-doc")
    public ResponseEntity<byte[]> judicialDoc(@PathVariable Long id, HttpServletRequest request) {
        String text = transferService.buildJudicialConfirmDoc(id, CurrentUser.get(request));
        String filename = URLEncoder.encode("司法确认申请书.txt", StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType("text/plain;charset=UTF-8"))
                .body(text.getBytes(StandardCharsets.UTF_8));
    }
}
