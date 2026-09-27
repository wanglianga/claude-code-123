package com.court.mediation.controller;

import com.court.mediation.common.R;
import com.court.mediation.entity.ImportBatch;
import com.court.mediation.repo.ImportBatchRepository;
import com.court.mediation.service.ImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/imports")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;
    private final ImportBatchRepository batchRepository;

    /** 上传 CSV 批量导入案件 */
    @PostMapping("/cases")
    public R<ImportBatch> importCases(@RequestParam("file") MultipartFile file) throws Exception {
        return R.ok(importService.importCsv(file));
    }

    @GetMapping("/batches")
    public R<List<ImportBatch>> batches() {
        return R.ok(batchRepository.findAllByOrderByCreatedAtDesc());
    }
}
