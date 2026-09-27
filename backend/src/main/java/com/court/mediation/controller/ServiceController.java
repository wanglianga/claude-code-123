package com.court.mediation.controller;

import com.court.mediation.common.R;
import com.court.mediation.entity.ServiceRecord;
import com.court.mediation.service.ServiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceService serviceService;

    /** 电子送达 */
    @PostMapping
    public R<ServiceRecord> serve(@RequestBody Map<String, String> body) {
        return R.ok(serviceService.serve(
                Long.valueOf(body.get("caseId")),
                body.get("documentName"),
                body.get("receiverName"),
                body.get("receiverPhone"),
                body.getOrDefault("channel", "ONLINE"),
                body.get("content")));
    }

    @PostMapping("/{id}/read")
    public R<ServiceRecord> read(@PathVariable Long id) {
        return R.ok(serviceService.markRead(id));
    }
}
