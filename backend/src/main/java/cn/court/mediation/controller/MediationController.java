package cn.court.mediation.controller;

import cn.court.mediation.common.ApiResponse;
import cn.court.mediation.entity.FaceVerification;
import cn.court.mediation.entity.MediationAgreement;
import cn.court.mediation.entity.MediationRecord;
import cn.court.mediation.security.CurrentUser;
import cn.court.mediation.service.MediationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class MediationController {

    private final MediationService mediationService;

    public MediationController(MediationService mediationService) {
        this.mediationService = mediationService;
    }

    /** 调解笔录：争议焦点、双方让步、结果 */
    @PostMapping("/cases/{id}/records")
    public ApiResponse<MediationRecord> saveRecord(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                                   HttpServletRequest request) {
        return ApiResponse.ok(mediationService.saveRecord(id, body, CurrentUser.get(request)));
    }

    /** 起草协议（付款计划、担保人、履行期限、司法确认意向） */
    @PostMapping("/cases/{id}/agreement")
    public ApiResponse<MediationAgreement> draft(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                                 HttpServletRequest request) {
        return ApiResponse.ok(mediationService.draftAgreement(id, body, CurrentUser.get(request)));
    }

    /** 人脸核验（模拟） */
    @PostMapping("/cases/{id}/face-verify")
    public ApiResponse<FaceVerification> faceVerify(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                                    HttpServletRequest request) {
        return ApiResponse.ok(mediationService.faceVerify(id, body, CurrentUser.get(request)));
    }

    /** 线上签署：party=APPLICANT/RESPONDENT */
    @PostMapping("/cases/{id}/sign")
    public ApiResponse<MediationAgreement> sign(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                                HttpServletRequest request) {
        return ApiResponse.ok(mediationService.sign(id, (String) body.getOrDefault("party", "APPLICANT"),
                CurrentUser.get(request)));
    }
}
