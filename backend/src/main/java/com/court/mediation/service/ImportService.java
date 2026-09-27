package com.court.mediation.service;

import com.court.mediation.entity.CaseEntity;
import com.court.mediation.entity.ImportBatch;
import com.court.mediation.repo.CaseRepository;
import com.court.mediation.repo.ImportBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 批量案件导入（CSV，UTF-8）。
 * 表头：标题,案由,诉求金额,申请人,申请人电话,被申请人,被申请人电话,被申请人地址,证据清单,愿意线上调解,紧急程度,是否外地
 */
@Service
@RequiredArgsConstructor
public class ImportService {

    private final CaseRepository caseRepository;
    private final ImportBatchRepository batchRepository;
    private final CaseService caseService;

    @Transactional
    public ImportBatch importCsv(MultipartFile file) throws Exception {
        ImportBatch batch = new ImportBatch();
        batch.setFileName(file.getOriginalFilename());
        batch.setOperatorId(com.court.mediation.security.CurrentUser.userId());
        batch = batchRepository.save(batch);
        final Long batchId = batch.getId();

        int total = 0, success = 0;
        List<String> fails = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean header = true;
            int row = 0;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                row++;
                if (header && line.contains("案由")) { header = false; continue; }
                header = false;
                total++;
                String[] f = parseCsv(line);
                try {
                    if (f.length < 7) throw new IllegalArgumentException("字段不足");
                    CaseEntity c = new CaseEntity();
                    c.setTitle(get(f, 0));
                    c.setCauseType(normalizeCause(get(f, 1)));
                    c.setClaimAmount(parseAmount(get(f, 2)));
                    c.setApplicantName(get(f, 3));
                    c.setApplicantPhone(get(f, 4));
                    c.setRespondentName(get(f, 5));
                    c.setRespondentPhone(get(f, 6));
                    c.setRespondentAddress(get(f, 7));
                    c.setEvidenceSummary(get(f, 8));
                    c.setWillingOnline(!"否".equals(get(f, 9)) && !"false".equalsIgnoreCase(get(f, 9)));
                    c.setUrgency(normalizeUrgency(get(f, 10)));
                    c.setRemoteParty("是".equals(get(f, 11)) || "true".equalsIgnoreCase(get(f, 11)));
                    CaseEntity saved = caseService.create(c);
                    saved.setImportBatchId(batchId);
                    caseRepository.save(saved);
                    success++;
                } catch (Exception ex) {
                    fails.add("第" + row + "行导入失败：" + ex.getMessage() + "（原始：" + line + "）");
                }
            }
        }
        batch.setTotalCount(total);
        batch.setSuccessCount(success);
        batch.setFailCount(fails.size());
        batch.setFailDetail(String.join("\n", fails));
        return batchRepository.save(batch);
    }

    /** 简易 CSV 解析（支持双引号包裹与逗号转义） */
    private static String[] parseCsv(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quote = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quote && i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                else quote = !quote;
            } else if (ch == ',' && !quote) {
                out.add(cur.toString().trim()); cur.setLength(0);
            } else cur.append(ch);
        }
        out.add(cur.toString().trim());
        return out.toArray(new String[0]);
    }

    private static String get(String[] arr, int i) {
        return i < arr.length ? arr[i].trim() : "";
    }

    private static BigDecimal parseAmount(String s) {
        if (s.isEmpty()) return BigDecimal.ZERO;
        return new BigDecimal(s.replaceAll("[^0-9.]", ""));
    }

    private static String normalizeCause(String s) {
        return List.of("民间借贷", "物业费", "交通事故", "邻里纠纷", "小额买卖合同").stream()
                .filter(x -> s.contains(x)).findFirst().orElse("邻里纠纷");
    }

    private static String normalizeUrgency(String s) {
        if (s.contains("特急")) return "VERY_URGENT";
        if (s.contains("紧急")) return "URGENT";
        return "NORMAL";
    }
}
