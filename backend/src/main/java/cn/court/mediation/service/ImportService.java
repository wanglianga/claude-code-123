package cn.court.mediation.service;

import cn.court.mediation.dto.CaseCreateRequest;
import cn.court.mediation.entity.MediationCase;
import cn.court.mediation.entity.User;
import cn.court.mediation.enums.CaseType;
import cn.court.mediation.enums.Urgency;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 批量案件导入（CSV）。
 * 表头：案由,申请人,申请人电话,被申请人,被申请人电话,被申请人地址,是否外地,诉求金额,诉求摘要,证据清单,愿意线上调解,紧急程度
 */
@Service
public class ImportService {

    private final CaseService caseService;

    public ImportService(CaseService caseService) {
        this.caseService = caseService;
    }

    @Transactional
    public Map<String, Object> importCsv(String csv, User operator) {
        List<String> errors = new ArrayList<>();
        int success = 0;
        List<String> caseNos = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new StringReader(csv))) {
            String line;
            int row = 0;
            boolean headerSkipped = false;
            while ((line = br.readLine()) != null) {
                row++;
                if (line.isBlank()) continue;
                String[] cells = parseCsvLine(line);
                if (!headerSkipped) {
                    headerSkipped = true;
                    // 首行若是表头（含“案由”字样）则跳过，否则按数据处理
                    if (cells[0].contains("案由")) continue;
                }
                try {
                    if (cells.length < 8) {
                        throw new IllegalArgumentException("列数不足，至少需要前8列");
                    }
                    CaseCreateRequest req = new CaseCreateRequest();
                    req.setCaseType(parseType(cells[0].trim()));
                    req.setApplicantName(cells[1].trim());
                    req.setApplicantPhone(cells[2].trim());
                    req.setRespondentName(cells[3].trim());
                    req.setRespondentPhone(cells[4].trim());
                    req.setRespondentAddress(cells.length > 5 ? cells[5].trim() : "");
                    req.setRespondentOutOfTown(cells.length > 6 && isYes(cells[6]));
                    req.setClaimAmount(new BigDecimal(cells[7].trim()));
                    req.setClaimSummary(cells.length > 8 ? cells[8].trim() : "");
                    req.setEvidenceList(cells.length > 9 ? cells[9].trim() : "");
                    req.setOnlineWilling(cells.length <= 10 || !"否".equals(cells[10].trim()));
                    req.setUrgency(cells.length > 11 ? parseUrgency(cells[11].trim()) : Urgency.NORMAL);
                    if (req.getApplicantName().isBlank() || req.getRespondentName().isBlank()) {
                        throw new IllegalArgumentException("当事人姓名不能为空");
                    }
                    MediationCase c = caseService.create(req, operator);
                    success++;
                    caseNos.add(c.getCaseNo());
                } catch (Exception ex) {
                    errors.add("第" + row + "行导入失败：" + ex.getMessage());
                }
            }
        } catch (Exception e) {
            errors.add("文件读取失败：" + e.getMessage());
        }
        return Map.of("success", success, "failed", errors.size(), "errors", errors, "caseNos", caseNos);
    }

    private CaseType parseType(String text) {
        for (CaseType t : CaseType.values()) {
            if (t.name().equals(text) || t.getLabel().contains(text) || text.contains(t.getLabel().replace("纠纷", ""))) {
                return t;
            }
        }
        throw new IllegalArgumentException("无法识别案由：" + text);
    }

    private Urgency parseUrgency(String text) {
        if (text.contains("特急")) return Urgency.EMERGENCY;
        if (text.contains("加急") || text.contains("紧急")) return Urgency.URGENT;
        return Urgency.NORMAL;
    }

    private boolean isYes(String v) {
        return v.contains("是") || v.equalsIgnoreCase("yes") || v.equals("1") || v.contains("外");
    }

    /** 支持引号包裹、含逗号字段的简单 CSV 解析 */
    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                result.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(ch);
            }
        }
        result.add(sb.toString());
        return result.toArray(new String[0]);
    }
}
