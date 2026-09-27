package com.court.mediation.controller;

import com.court.mediation.common.R;
import com.court.mediation.entity.VisitRecord;
import com.court.mediation.service.VisitService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/visits")
@RequiredArgsConstructor
public class VisitController {

    private final VisitService visitService;

    @PostMapping
    public R<VisitRecord> save(@RequestBody VisitRecord visit) {
        return R.ok(visitService.save(visit));
    }
}
