package com.court.mediation.service;

import com.court.mediation.common.BizException;
import com.court.mediation.entity.VisitRecord;
import com.court.mediation.repo.VisitRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 履行回访：电话/线上/上门回访，记录按约履行、部分履行、未履行或提出异议 */
@Service
@RequiredArgsConstructor
public class VisitService {

    private final VisitRecordRepository repository;
    private final TimelineService timelineService;

    @Transactional
    public VisitRecord save(VisitRecord v) {
        if (v.getCaseId() == null) throw new BizException("缺少案件");
        VisitRecord saved = repository.save(v);
        timelineService.record(v.getCaseId(), "VISIT",
                "履行回访（" + channelText(v.getChannel()) + "，对象：" + nz(v.getTarget()) + "）结果："
                        + resultText(v.getResult()) + "。" + nz(v.getContent()), saved.getId());
        return saved;
    }

    public static String resultText(String r) {
        return switch (r == null ? "" : r) {
            case "PERFORMED" -> "已按约履行";
            case "PARTIAL" -> "部分履行";
            case "BREACH" -> "未履行/违约";
            case "OBJECTION" -> "提出异议";
            default -> r;
        };
    }

    public static String channelText(String c) {
        return switch (c == null ? "" : c) {
            case "ONLINE" -> "线上";
            case "ONSITE" -> "上门";
            default -> "电话";
        };
    }

    private static String nz(String s) {
        return s == null || s.isBlank() ? "" : s;
    }
}
