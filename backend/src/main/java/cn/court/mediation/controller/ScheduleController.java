package cn.court.mediation.controller;

import cn.court.mediation.common.ApiResponse;
import cn.court.mediation.entity.MediationSchedule;
import cn.court.mediation.security.CurrentUser;
import cn.court.mediation.service.ScheduleService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    /** 智能推荐调解员（专长/在办量/时间冲突/外地线上） */
    @GetMapping("/cases/{id}/mediator-suggest")
    public ApiResponse<List<Map<String, Object>>> suggest(@PathVariable Long id,
                                                          @RequestParam(required = false) String start,
                                                          @RequestParam(required = false) String end) {
        LocalDateTime s = start == null ? null : LocalDateTime.parse(start);
        LocalDateTime e = end == null ? null : LocalDateTime.parse(end);
        return ApiResponse.ok(scheduleService.recommendMediators(id, s, e));
    }

    @PostMapping("/cases/{id}/schedules")
    public ApiResponse<MediationSchedule> create(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                                 HttpServletRequest request) {
        return ApiResponse.ok(scheduleService.create(id, body, CurrentUser.get(request)));
    }

    @GetMapping("/cases/{id}/schedules")
    public ApiResponse<List<MediationSchedule>> list(@PathVariable Long id) {
        return ApiResponse.ok(scheduleService.caseSchedules(id));
    }

    /** 改期（原记录保留留痕） */
    @PostMapping("/schedules/{id}/reschedule")
    public ApiResponse<MediationSchedule> reschedule(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                                     HttpServletRequest request) {
        return ApiResponse.ok(scheduleService.reschedule(id, body, CurrentUser.get(request)));
    }

    @PostMapping("/schedules/{id}/confirm")
    public ApiResponse<Void> confirm(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                     HttpServletRequest request) {
        scheduleService.confirmAttend(id, (String) body.getOrDefault("party", "APPLICANT"),
                CurrentUser.get(request));
        return ApiResponse.ok();
    }
}
