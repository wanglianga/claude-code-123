package cn.court.mediation.controller;

import cn.court.mediation.common.ApiResponse;
import cn.court.mediation.entity.DeliveryRecord;
import cn.court.mediation.entity.FollowUp;
import cn.court.mediation.security.CurrentUser;
import cn.court.mediation.service.DeliveryService;
import cn.court.mediation.service.FollowUpService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cases/{caseId}")
public class CaseActionController {

    private final FollowUpService followUpService;
    private final DeliveryService deliveryService;

    public CaseActionController(FollowUpService followUpService, DeliveryService deliveryService) {
        this.followUpService = followUpService;
        this.deliveryService = deliveryService;
    }

    @PostMapping("/follow-ups")
    public ApiResponse<FollowUp> followUp(@PathVariable Long caseId, @RequestBody Map<String, Object> body,
                                          HttpServletRequest request) {
        return ApiResponse.ok(followUpService.create(caseId, body, CurrentUser.get(request)));
    }

    @PostMapping("/deliveries")
    public ApiResponse<DeliveryRecord> deliver(@PathVariable Long caseId, @RequestBody Map<String, Object> body,
                                               HttpServletRequest request) {
        return ApiResponse.ok(deliveryService.send(caseId, body, CurrentUser.get(request)));
    }
}
