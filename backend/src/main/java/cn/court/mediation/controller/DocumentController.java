package cn.court.mediation.controller;

import cn.court.mediation.security.CurrentUser;
import cn.court.mediation.service.TransferService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** 连续过程材料卷下载：多次改期/失联/部分履行时一键导出提交法官 */
@RestController
@RequestMapping("/api/cases/{id}")
public class DocumentController {

    private final TransferService transferService;

    public DocumentController(TransferService transferService) {
        this.transferService = transferService;
    }

    @GetMapping("/dossier")
    public ResponseEntity<byte[]> dossier(@PathVariable Long id, HttpServletRequest request) {
        String text = transferService.dossierForCase(id, CurrentUser.get(request));
        String filename = URLEncoder.encode("诉前调解过程材料卷.txt", StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType("text/plain;charset=UTF-8"))
                .body(text.getBytes(StandardCharsets.UTF_8));
    }
}
