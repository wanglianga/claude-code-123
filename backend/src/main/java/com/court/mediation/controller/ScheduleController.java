package com.court.mediation.controller;

import com.court.mediation.common.R;
import com.court.mediation.entity.Schedule;
import com.court.mediation.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    /** 按案由推荐调解员（专长匹配） */
    @GetMapping("/mediators")
    public R<List<Map<String, Object>>> mediators(@RequestParam(required = false) String causeType) {
        return R.ok(scheduleService.recommendMediators(causeType));
    }

    /** 排期或改期（同一案件再次调用即改期） */
    @PostMapping
    public R<Schedule> create(@RequestBody Map<String, Object> body) {
        Long caseId = Long.valueOf(String.valueOf(body.get("caseId")));
        LocalDateTime start = LocalDateTime.parse(String.valueOf(body.get("start")));
        LocalDateTime end = body.get("end") == null ? null : LocalDateTime.parse(String.valueOf(body.get("end")));
        String mode = body.get("mode") == null ? null : String.valueOf(body.get("mode"));
        String location = body.get("location") == null ? null : String.valueOf(body.get("location"));
        String note = body.get("note") == null ? null : String.valueOf(body.get("note"));
        boolean confirmed = Boolean.parseBoolean(String.valueOf(body.getOrDefault("confirmed", "false")));
        return R.ok(scheduleService.create(caseId, start, end, mode, location, note, confirmed));
    }

    @PostMapping("/{id}/confirm")
    public R<Schedule> confirm(@PathVariable Long id) {
        return R.ok(scheduleService.confirm(id));
    }

    @PostMapping("/{id}/complete")
    public R<Schedule> complete(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        return R.ok(scheduleService.complete(id, body == null ? null : body.get("note")));
    }
}
